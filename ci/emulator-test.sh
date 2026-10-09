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

dump() {
  for i in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 && adb pull /sdcard/ui.xml "$CUR" > /dev/null 2>&1 && return 0
    sleep 2
  done
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

shot() { adb exec-out screencap -p > "ci-results/$1.png"; }

adb shell settings put global hide_error_dialogs 1 > /dev/null 2>&1
adb install -r "$APK" 2>&1 | tee ci-results/install.log
adb logcat -c
adb shell am start -W -n $PKG/$ACT 2>&1 | tee ci-results/start.log

echo "== 1) Catalogo"
expect "catalogo caricato dal database" "Disponibile|Non disponibile" 120
shot 01-home

echo "== 2) Scheda del vino e preferito (come ospite)"
tap "Non disponibile" 20 || tap "Disponibile" 20
expect "scheda del vino aperta" "Indietro" 30
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

adb logcat -d > ci-results/logcat.txt
if grep -q "FATAL EXCEPTION" ci-results/logcat.txt; then bad "crash dell'app"; else ok "nessun crash"; fi

echo "$FAIL" > ci-results/emulator-exit.txt
exit $FAIL
