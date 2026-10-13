#!/bin/bash
# Prova sull'emulatore: percorre le funzioni principali dell'app (catalogo, scheda, preferiti,
# persistenza dopo la chiusura, carrello, account, assistenza) senza inviare richieste vere.
mkdir -p ci-results
exec > >(tee ci-results/emulator-test.log) 2>&1
APK=app/build/outputs/apk/debug/app-debug.apk
PKG=mywine.cellar
ACT=dev.pages.mywinecellar.MainActivity
CUR=ci-results/ui-current.xml
FAIL=0

ok()  { echo "OK: $1"; }
bad() { echo "ERRORE: $1"; FAIL=1; }

# L'emulatore a volte si blocca (succede di rado): se non risponde più ci si ferma subito e il flusso riprova da capo.
emulator_alive() { timeout 15 adb shell echo ok 2>/dev/null | grep -q ok; }
abort_if_dead() {
  if ! emulator_alive; then
    echo "ERRORE GRAVE: l'emulatore non risponde più, interrompo questa prova"
    touch ci-results/emulator-dead.txt
    echo "EMULATOR_DEAD=1" >> "$GITHUB_ENV"
    echo 1 > ci-results/emulator-exit.txt
    echo "--- ultime righe del registro del telefono prima del blocco:"
    tail -40 ci-results/logcat-live.txt 2>/dev/null | cut -c1-220
    exit 99
  fi
}

dump() {
  for i in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 && adb pull /sdcard/ui.xml "$CUR" > /dev/null 2>&1 && return 0
    sleep 2
  done
  abort_if_dead
  return 1
}

# Aspetta che compaia uno dei testi (alternative separate da |). Intanto chiude le finestre di sistema.
wait_for() {
  local end=$((SECONDS + $2))
  while [ $SECONDS -lt $end ]; do
    if dump; then
      if python3 ci/ui.py has "$CUR" "$1"; then return 0; fi
      for b in "Wait" "Riprova"; do
        if xy=$(python3 ci/ui.py tap "$CUR" "$b"); then echo "  tocco su '$b'"; adb shell input tap $xy; sleep 2; fi
      done
    fi
    sleep 2
  done
  return 1
}

expect() { if wait_for "$2" "${3:-30}"; then ok "$1"; return 0; else bad "$1 (non compare: $2)"; return 1; fi; }

# Tocca l'elemento con quel testo (o descrizione), aspettando che compaia.
tap() {
  local end=$((SECONDS + ${2:-25}))
  while [ $SECONDS -lt $end ]; do
    if dump && xy=$(python3 ci/ui.py tap "$CUR" "$1"); then adb shell input tap $xy; sleep 2; return 0; fi
    sleep 2
  done
  bad "elemento non trovato: $1"
  return 1
}

shot() { adb exec-out screencap -p > "ci-results/$1.png"; [ -s "ci-results/$1.png" ] || abort_if_dead; }

# Chiude la tastiera solo se è davvero aperta (altrimenti il tasto indietro cambierebbe schermata).
hide_keyboard() {
  if adb shell dumpsys input_method 2>/dev/null | grep -q "mInputShown=true"; then adb shell input keyevent KEYCODE_BACK; fi
  sleep 2
}

# Tocca il campo con quell'etichetta, svuota il contenuto e scrive il testo (gli spazi diventano %s per adb).
type_into() {
  tap "$1" 15 || return 1
  adb shell input keyevent KEYCODE_MOVE_END
  for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30; do adb shell input keyevent KEYCODE_DEL; done
  adb shell input text "$(printf '%s' "$2" | sed 's/ /%s/g')"
  sleep 1
  hide_keyboard
}

# Controlli sullo stato del server simulato (solo per la prova con la variante "fake").
fake_state() { curl -s localhost:8787/_state; }
fake_check() { # descrizione, condizione in Python sulla variabile s (lo stato)
  if fake_state | python3 -c "import sys,json; s=json.load(sys.stdin); sys.exit(0 if ($2) else 1)"; then ok "$1"; else bad "$1 (server: $(fake_state | head -c 300))"; fi
}

adb shell settings put global hide_error_dialogs 1 > /dev/null 2>&1
# Registro del telefono scritto in continuo: se l'emulatore si blocca resta comunque tutto fino a quel momento.
adb logcat -c
(adb logcat -v time > ci-results/logcat-live.txt 2>&1 &)
adb install -r "$APK" 2>&1 | tee ci-results/install.log
adb logcat -c
adb shell am start -W -n $PKG/$ACT 2>&1 | tee ci-results/start.log

echo "== 1) Catalogo"
expect "catalogo caricato dal database" "Disponibile|Non disponibile" 120
shot 01-home

echo "== 2) Scheda del vino e preferito (come ospite)"
tap "Non disponibile" 20 || tap "Disponibile" 20
expect "scheda del vino aperta" "Indietro" 30
# La scheda è lunga: scorro verso il basso finché compare "Altri vini di ..." (massimo 5 volte), poi torno in cima.
FOUND=0
for k in 1 2 3 4 5; do
  dump
  if python3 ci/ui.py has "$CUR" "^Altri vini di"; then FOUND=1; break; fi
  adb shell input swipe 540 1800 540 500 300
  sleep 1
done
if [ $FOUND = 1 ]; then ok "altri vini della stessa cantina"; shot 02b-altri-vini; else bad "altri vini della stessa cantina (non compare: ^Altri vini di)"; fi
for k in 1 2 3 4 5 6; do adb shell input swipe 540 500 540 1800 200; done
sleep 1
tap "Aggiungi ai preferiti" 20
expect "avviso per gli ospiti" "Stai usando l'app come ospite" 20
shot 02-guest-hint
tap "Continua come ospite" 15
expect "cuore attivo (preferito salvato)" "Rimuovi dai preferiti" 30
shot 03-detail-favorite
tap "Indietro" 15

echo "== 3) Elenco preferiti"
tap "Preferiti" 15
expect "il vino è tra i preferiti" "Disponibile|Non disponibile" 30
shot 04-favorites

echo "== 4) Chiudo e riapro l'app: il preferito deve restare"
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/$ACT > /dev/null 2>&1
expect "catalogo ricaricato" "Disponibile|Non disponibile" 120
tap "Preferiti" 15
expect "il preferito è ancora lì dopo la riapertura" "Disponibile|Non disponibile" 60
shot 05-favorites-after-restart
tap "Rimuovi dai preferiti" 15
expect "preferito rimosso" "^Nessun preferito" 30

echo "== 4a) Gruppi per cantina, ricerca e aggiornamento"
tap "Catalogo" 15
expect "elenco" "Solo disponibili" 30
expect "intestazione della cantina" "Accordini Igino" 20
dump; NA=$(python3 ci/ui.py count "$CUR" -)
if xy=$(python3 ci/ui.py tap "$CUR" "Cerca cantina, vino, annata…"); then
  adb shell input tap $xy; sleep 1
  adb shell input text "Amarone"; sleep 2
  adb shell input keyevent KEYCODE_ENTER; sleep 2
  dump; NB=$(python3 ci/ui.py count "$CUR" -)
  echo "vini: prima=$NA, cercando Amarone=$NB"
  if [ -n "$NB" ] && [ "$NB" -gt 0 ] && [ "$NB" -lt "$NA" ]; then ok "la ricerca restringe l'elenco"; else bad "la ricerca non funziona (prima=$NA, dopo=$NB)"; fi
  shot 04a-ricerca
  tap "Cancella ricerca" 15
  dump; NC=$(python3 ci/ui.py count "$CUR" -)
  if [ "$NC" = "$NA" ]; then ok "cancellando la ricerca tornano tutti i vini"; else bad "dopo aver cancellato la ricerca: $NC invece di $NA"; fi
else
  echo "SALTATO: campo di ricerca non trovato nello schermo"
fi
adb shell input swipe 540 900 540 1700 400
sleep 3
expect "l'elenco è ancora lì dopo l'aggiornamento" "Disponibile|Non disponibile" 60

echo "== 4b) Filtri"
tap "Catalogo" 15
expect "elenco" "Solo disponibili" 30
dump; N0=$(python3 ci/ui.py count "$CUR" -)
tap "Filtri" 15
expect "finestra dei filtri" "Applica" 20
tap "Rosso" 15
tap "Applica" 15
expect "filtro attivo" "^Filtri (1)" 20
dump; N1=$(python3 ci/ui.py count "$CUR" -)
echo "vini: prima=$N0, con filtro Rosso=$N1"
if [ -n "$N0" ] && [ -n "$N1" ] && [ "$N1" -gt 0 ] && [ "$N1" -le "$N0" ]; then ok "il filtro per colore restringe l'elenco"; else bad "il filtro per colore non funziona (prima=$N0, dopo=$N1)"; fi
shot 04b-filtri
tap "Filtri (1)" 15
tap "Azzera tutto" 15
tap "Applica" 15
expect "filtri azzerati" "Filtri" 20
dump; N2=$(python3 ci/ui.py count "$CUR" -)
if [ "$N2" = "$N0" ]; then ok "azzerando i filtri tornano tutti i vini ($N2)"; else bad "dopo l'azzeramento: $N2 invece di $N0"; fi

echo "== 5) Carrello"
tap "Catalogo" 15
expect "elenco" "Solo disponibili" 30
tap "Solo disponibili" 15
sleep 3
dump
if python3 ci/ui.py has "$CUR" "Disponibile"; then
  tap "Disponibile" 15
  expect "scheda con pulsante carrello" "Aggiungi al carrello" 30
  tap "Aggiungi al carrello" 15
  expect "aggiunto al carrello" "^Nel carrello" 20
  tap "Indietro" 15
  tap "Carrello" 15
  expect "carrello con totale o prezzo da concordare" "^Totale stimato|^Prezzo da concordare" 30
  shot 06-cart
  tap "Invia richiesta" 15
  expect "finestra della richiesta" "Invia la richiesta" 20
  tap "Invia" 15
  expect "controllo dei dati obbligatori" "Email e telefono sono entrambi obbligatori." 20
  shot 07-cart-validation
  tap "Annulla" 15
  tap "−" 15
  expect "carrello svuotato" "^Il carrello è vuoto" 30
else
  echo "SALTATO: nessun vino disponibile nel catalogo"
fi

echo "== 6) Account e assistenza"
tap "Cantina" 15
expect "schermata account da ospite" "Salva la tua cantina" 30
tap "Salva la mia cantina" 15
expect "controllo email" "Inserisci un indirizzo email valido." 20
tap "Hai già un account? Accedi" 15
expect "modulo di accesso" "Accedi alla tua cantina" 20
shot 08-account
tap "Scrivici un messaggio" 15
expect "finestra assistenza" "Scrivi all'assistenza" 20
tap "Invia" 15
expect "controllo messaggio" "Scrivi un messaggio prima di inviare." 20
shot 09-support-validation
tap "Annulla" 15

if [ -f ci/account-test.enabled ]; then
  echo "== 6b) Ciclo completo dell'account sul server VERO (salva, esci, accedi, elimina)"
  # Questa prova tocca il database vero: se qualcosa non va è un AVVISO, non blocca la pubblicazione dell'APK.
  bad() { echo "AVVISO (prova sul server vero): $1"; }
  TESTMAIL="ci-${GITHUB_RUN_ID:-$RANDOM}-$RANDOM@example.invalid"
  tap "Cantina" 15
  expect "schermata account da ospite" "Salva la tua cantina|Accedi alla tua cantina" 30
  if python3 ci/ui.py has "$CUR" "Accedi alla tua cantina"; then tap "Non hai un account? Salva la tua cantina" 15; fi
  type_into "Email" "$TESTMAIL"
  shot 08a-email-scritta
  tap "Salva la mia cantina" 15
  expect "account creato: sei collegato" "^Sei collegato come" 60
  shot 08b-account-creato
  tap "Esci da questo dispositivo" 15
  tap "Sì, esci" 15
  expect "uscito: di nuovo ospite" "Salva la tua cantina" 60
  tap "Hai già un account? Accedi" 15
  type_into "Email" "$TESTMAIL"
  tap "Accedi" 15
  expect "accesso con la stessa email" "^Sei collegato come" 60
  tap "Elimina il mio account" 15
  tap "Elimina" 15
  expect "account eliminato: di nuovo ospite" "Salva la tua cantina" 60
  shot 08c-account-eliminato
  bad() { echo "ERRORE: $1"; FAIL=1; }
  echo "--- registro dell'app durante la prova sul server vero:"
  adb logcat -d -s CantinaApi:I > ci-results/cantina-api.log 2>&1
  tail -30 ci-results/cantina-api.log
fi

echo "== 6c) Tema scuro e testo ingrandito"
adb shell cmd uimode night yes > /dev/null 2>&1
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/$ACT > /dev/null 2>&1
expect "catalogo con tema scuro" "Disponibile|Non disponibile" 90
shot 11-scuro-home
tap "Non disponibile" 20 || tap "Disponibile" 20
expect "scheda con tema scuro" "Indietro" 30
shot 12-scuro-scheda
adb shell cmd uimode night no > /dev/null 2>&1
adb shell settings put system font_scale 1.5 > /dev/null 2>&1
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/$ACT > /dev/null 2>&1
expect "catalogo con testo ingrandito" "Disponibile|Non disponibile" 90
shot 13-testo-grande
tap "Cantina" 20
expect "schermata account con testo ingrandito" "Salva la tua cantina|Accedi alla tua cantina" 30
shot 14-testo-grande-account
adb shell settings put system font_scale 1.0 > /dev/null 2>&1
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/$ACT > /dev/null 2>&1
expect "catalogo di nuovo normale" "Disponibile|Non disponibile" 90

echo "== 6e) Schermo largo (tablet) e telefono in orizzontale"
adb shell wm size 1600x2560 > /dev/null 2>&1
adb shell wm density 320 > /dev/null 2>&1
sleep 3
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/$ACT > /dev/null 2>&1
expect "catalogo su schermo largo" "Disponibile|Non disponibile" 90
shot 19-tablet-catalogo
tap "Non disponibile" 20 || tap "Disponibile" 20
expect "scheda su schermo largo" "Indietro" 30
shot 20-tablet-scheda
adb shell wm size reset > /dev/null 2>&1
adb shell wm density reset > /dev/null 2>&1
adb shell settings put system accelerometer_rotation 0 > /dev/null 2>&1
adb shell settings put system user_rotation 1 > /dev/null 2>&1
sleep 3
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/$ACT > /dev/null 2>&1
expect "catalogo in orizzontale" "Disponibile|Non disponibile" 90
shot 21-orizzontale
adb shell settings put system user_rotation 0 > /dev/null 2>&1
adb shell settings put system accelerometer_rotation 1 > /dev/null 2>&1
sleep 2

FAPK=app/build/outputs/apk/fake/app-fake.apk
FPKG=mywine.cellar.fake
if [ -f "$FAPK" ] && curl -s localhost:8787/_state > /dev/null 2>&1; then
  echo "== 6d) Prove con server simulato: accesso con codice, richieste, eliminazione"
  adb install -r "$FAPK" 2>&1 | tail -1
  adb shell pm clear $FPKG > /dev/null 2>&1
  curl -s -X POST localhost:8787/_mode -d '{"mode":"code"}' > /dev/null
  adb shell am start -W -n $FPKG/$ACT > /dev/null 2>&1
  expect "catalogo dal server simulato" "Disponibile|Non disponibile" 60

  echo "-- salvare la cantina con il codice"
  tap "Cantina" 15
  expect "schermata account da ospite" "Salva la tua cantina" 30
  type_into "Email" "prova@esempio.it"
  tap "Salva la mia cantina" 15
  expect "passo del codice" "Controlla la tua email" 30
  shot 15-passo-codice
  type_into "Codice" "000000"
  tap "Conferma" 15
  expect "codice sbagliato rifiutato" "Codice non corretto." 20
  CODE=$(fake_state | python3 -c "import sys,json; print(json.load(sys.stdin)['lastCode'])")
  type_into "Codice" "$CODE"
  tap "Conferma" 15
  expect "cantina salvata: sei collegato" "^Sei collegato come" 30
  fake_check "il server ha collegato l'email all'account" "any(u['email']=='prova@esempio.it' for u in s['users'])"
  shot 16-collegato

  echo "-- richiesta dal carrello"
  tap "Catalogo" 15
  expect "elenco" "Solo disponibili" 30
  tap "Solo disponibili" 15
  sleep 2
  tap "Disponibile" 15
  expect "scheda con pulsante carrello" "Aggiungi al carrello" 30
  tap "Aggiungi al carrello" 15
  tap "Indietro" 15
  tap "Carrello" 15
  expect "carrello con il vino" "^Totale stimato|^Prezzo da concordare" 30
  tap "Invia richiesta" 15
  expect "finestra della richiesta" "Invia la richiesta" 20
  type_into "Telefono (obbligatorio)" "3331234567"
  tap "Invia" 15
  expect "carrello svuotato dopo l'invio" "^Il carrello è vuoto" 30
  fake_check "il server ha ricevuto la richiesta del carrello" "any(r.get('type')=='cart' and r['phone']=='3331234567' and r['email']=='prova@esempio.it' and len(r['items'])==1 for r in s['requests'])"

  echo "-- richiesta di disponibilità"
  tap "Catalogo" 15
  expect "elenco" "Solo disponibili" 30
  tap "Tutti" 15
  sleep 2
  tap "Non disponibile" 15
  expect "scheda del vino non disponibile" "Richiedi disponibilità" 30
  tap "Richiedi disponibilità" 15
  expect "finestra della richiesta" "Invia" 20
  tap "Invia" 15
  expect "conferma di invio" "^Richiesta inviata" 30
  fake_check "il server ha ricevuto la richiesta di disponibilità" "any(r.get('type')=='availability' and r['wine_id']=='w2' for r in s['requests'])"
  tap "Indietro" 15

  echo "-- assistenza: invio riuscito e limite di richieste"
  tap "Cantina" 15
  tap "Scrivici un messaggio" 15
  type_into "Messaggio" "Ciao dal test"
  tap "Invia" 15
  expect "messaggio inviato" "^Messaggio inviato" 30
  fake_check "il server ha ricevuto il messaggio" "any(r.get('type')=='support' and r['message']=='Ciao dal test' for r in s['requests'])"
  tap "Scrivici un messaggio" 15
  type_into "Messaggio" "LIMITE"
  tap "Invia" 15
  expect "messaggio del limite di richieste" "^Hai inviato troppe richieste" 30
  shot 17-limite-richieste
  tap "Annulla" 15

  echo "-- uscire e rientrare con il codice"
  tap "Esci da questo dispositivo" 15
  tap "Sì, esci" 15
  expect "di nuovo ospite" "Salva la tua cantina" 30
  tap "Hai già un account? Accedi" 15
  type_into "Email" "prova@esempio.it"
  tap "Accedi" 15
  expect "passo del codice per l'accesso" "Controlla la tua email" 30
  CODE=$(fake_state | python3 -c "import sys,json; print(json.load(sys.stdin)['lastCode'])")
  type_into "Codice" "$CODE"
  tap "Conferma" 15
  expect "rientrato con il codice" "^Sei collegato come" 30

  echo "-- eliminare l'account"
  tap "Elimina il mio account" 15
  tap "Elimina" 15
  expect "account eliminato: di nuovo ospite" "Salva la tua cantina" 30
  fake_check "il server ha eliminato l'utente" "not any(u['email']=='prova@esempio.it' for u in s['users'])"

  echo "-- modalità attuale (senza codice): tutto come prima"
  curl -s -X POST localhost:8787/_mode -d '{"mode":"legacy"}' > /dev/null
  type_into "Email" "vecchia@esempio.it"
  tap "Salva la mia cantina" 15
  expect "salvataggio immediato senza codice" "^Sei collegato come" 30
  shot 18-modalita-attuale
  adb shell am force-stop $FPKG
else
  echo "SALTATO: variante di prova o server simulato non disponibili"
fi

echo "== 7) Apertura senza connessione (mostra l'ultimo catalogo salvato)"
adb shell svc wifi disable > /dev/null 2>&1
adb shell svc data disable > /dev/null 2>&1
sleep 3
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/$ACT > /dev/null 2>&1
expect "avviso di catalogo non aggiornato" "^Catalogo non aggiornato" 90
expect "i vini si vedono anche offline" "Disponibile|Non disponibile" 30
shot 10-offline
adb shell svc wifi enable > /dev/null 2>&1
adb shell svc data enable > /dev/null 2>&1

adb logcat -d > ci-results/logcat.txt
if grep -q "FATAL EXCEPTION" ci-results/logcat.txt; then bad "crash dell'app"; else ok "nessun crash"; fi

echo "$FAIL" > ci-results/emulator-exit.txt
exit $FAIL
