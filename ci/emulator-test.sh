#!/bin/bash
# Prova sull'emulatore: installa l'app, aspetta che il catalogo si carichi, apre la scheda di un vino,
# torna indietro, fa gli screenshot e controlla che l'app non vada in crash.
mkdir -p ci-results
exec > >(tee ci-results/emulator-test.log) 2>&1
APK=app/build/outputs/apk/debug/app-debug.apk
PKG=dev.pages.mywinecellar
CUR=ci-results/ui-current.xml
FAIL=0

dump() {
  for i in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 && adb pull /sdcard/ui.xml "$CUR" > /dev/null 2>&1 && return 0
    sleep 2
  done
  return 1
}

# Aspetta che compaia uno dei testi indicati (alternative separate da |). Intanto chiude finestre di sistema e preme "Riprova".
wait_for() {
  local end=$((SECONDS + $2))
  while [ $SECONDS -lt $end ]; do
    if dump; then
      if python3 ci/ui.py has "$CUR" "$1"; then return 0; fi
      for b in "Wait" "Riprova"; do
        if xy=$(python3 ci/ui.py tap "$CUR" "$b"); then echo "  tocco su '$b'"; adb shell input tap $xy; sleep 2; fi
      done
    fi
    sleep 3
  done
  return 1
}

shot() { adb exec-out screencap -p > "ci-results/$1.png"; }

# Niente finestre di errore di sistema che coprono l'app (l'emulatore di prova è lento)
adb shell settings put global hide_error_dialogs 1 > /dev/null 2>&1

adb install -r "$APK" 2>&1 | tee ci-results/install.log
adb logcat -c
adb shell am start -W -n $PKG/.MainActivity 2>&1 | tee ci-results/start.log

echo "1) Attendo il catalogo (massimo 120 secondi)"
if wait_for "Disponibile|Non disponibile" 120; then echo "OK: catalogo caricato"; else echo "ERRORE: il catalogo non si è caricato"; FAIL=1; fi
shot 01-home

echo "2) Apro la scheda del primo vino"
if xy=$(python3 ci/ui.py tap "$CUR" "Disponibile") || xy=$(python3 ci/ui.py tap "$CUR" "Non disponibile"); then
  adb shell input tap $xy
  if wait_for "Indietro" 30; then echo "OK: scheda del vino aperta"; else echo "ERRORE: la scheda del vino non si apre"; FAIL=1; fi
else
  echo "ERRORE: nessuna scheda da aprire"; FAIL=1
fi
shot 02-detail

echo "3) Torno all'elenco"
adb shell input keyevent KEYCODE_BACK
if wait_for "Solo disponibili" 30; then echo "OK: tornato all'elenco"; else echo "ERRORE: il tasto indietro non riporta all'elenco"; FAIL=1; fi
shot 03-back

adb logcat -d > ci-results/logcat.txt
if grep -q "FATAL EXCEPTION" ci-results/logcat.txt; then echo "ERRORE: crash dell'app"; FAIL=1; else echo "OK: nessun crash"; fi

echo "$FAIL" > ci-results/emulator-exit.txt
exit $FAIL
