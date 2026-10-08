#!/bin/bash
# Prova di base sull'emulatore: installa l'app, la avvia, fa gli screenshot e controlla che non vada in crash.
mkdir -p ci-results
exec > >(tee ci-results/emulator-test.log) 2>&1
APK=app/build/outputs/apk/debug/app-debug.apk
PKG=dev.pages.mywinecellar
FAIL=0

# Attende che il telefono virtuale riesca a risolvere l'indirizzo del database (fino a 90 secondi)
for i in $(seq 1 30); do
  if adb shell ping -c 1 -W 2 xqhoznbjyoityjtrolzb.supabase.co > /dev/null 2>&1; then echo "Rete dell'emulatore pronta (tentativo $i)"; break; fi
  echo "Rete dell'emulatore non ancora pronta (tentativo $i)"; sleep 3
done
adb shell ping -c 1 -W 3 xqhoznbjyoityjtrolzb.supabase.co 2>&1 | head -3

adb install -r "$APK" 2>&1 | tee ci-results/install.log
adb logcat -c
adb shell am start -W -n $PKG/.MainActivity 2>&1 | tee ci-results/start.log
sleep 25

adb exec-out screencap -p > ci-results/01-home.png
adb shell uiautomator dump /sdcard/ui-home.xml > /dev/null 2>&1
adb pull /sdcard/ui-home.xml ci-results/ui-home.xml > /dev/null 2>&1

# Il catalogo deve essere stato caricato dal database: cerchiamo almeno una scheda con "Disponibile" / "Non disponibile".
if grep -q 'text="La Cantina di Egon"' ci-results/ui-home.xml; then echo "OK: titolo presente"; else echo "ERRORE: titolo non trovato"; FAIL=1; fi
if grep -Eq 'text="(Non d|D)isponibile"' ci-results/ui-home.xml; then echo "OK: almeno un vino mostrato"; else echo "ERRORE: nessun vino mostrato (rete o dati?)"; FAIL=1; fi

# Apre la scheda del primo vino (tocco al centro della prima scheda) e fa lo screenshot.
adb shell input tap 540 640
sleep 3
adb exec-out screencap -p > ci-results/02-detail.png
adb shell uiautomator dump /sdcard/ui-detail.xml > /dev/null 2>&1
adb pull /sdcard/ui-detail.xml ci-results/ui-detail.xml > /dev/null 2>&1
if grep -q 'content-desc="Indietro"' ci-results/ui-detail.xml; then echo "OK: scheda del vino aperta"; else echo "ERRORE: scheda del vino non aperta"; FAIL=1; fi

adb shell input keyevent KEYCODE_BACK
sleep 2
adb exec-out screencap -p > ci-results/03-back.png

adb logcat -d > ci-results/logcat.txt
if grep -q "FATAL EXCEPTION" ci-results/logcat.txt; then echo "ERRORE: crash dell'app"; FAIL=1; fi

echo "$FAIL" > ci-results/emulator-exit.txt
exit $FAIL
