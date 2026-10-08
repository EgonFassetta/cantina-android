#!/bin/bash
# Raccoglie i risultati e li salva nel ramo "ci-results" del repository, dove possono essere letti.
set +e
OUT=ci-results
mkdir -p "$OUT"

{
  echo "# Risultati CI — run n. $GITHUB_RUN_NUMBER"
  echo
  echo "- Commit: $GITHUB_SHA"
  echo "- Compilazione e test automatici: **$BUILD_OUTCOME**"
  echo "- Prova sull'emulatore: **$EMU_OUTCOME**"
  echo "- Data: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo
  echo "## Test automatici (logica)"
  python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
t = f = e = s = 0
for p in glob.glob('app/build/test-results/testDebugUnitTest/*.xml'):
    r = ET.parse(p).getroot()
    t += int(r.get('tests', 0)); f += int(r.get('failures', 0)); e += int(r.get('errors', 0)); s += int(r.get('skipped', 0))
    for tc in r.findall('testcase'):
        for bad in tc.findall('failure') + tc.findall('error'):
            print('FALLITO:', tc.get('classname'), tc.get('name'), '-', (bad.get('message') or '')[:300])
print(f'test eseguiti: {t}, falliti: {f}, errori: {e}, saltati: {s}')
PY
  echo
  echo "## Errori di compilazione"
  grep -E "^e: |error:|FAILURE:|What went wrong|Execution failed|Could not " -A4 "$OUT/build.log" | head -150
  echo
  echo "## Ultime righe del log di compilazione"
  tail -40 "$OUT/build.log"
  echo
  echo "## Prova sull'emulatore"
  if [ -f "$OUT/emulator-exit.txt" ]; then echo "esito: $(cat $OUT/emulator-exit.txt) (0 = ok)"; else echo "non eseguita o interrotta"; fi
  if [ -f "$OUT/logcat.txt" ]; then
    echo
    echo "### Errori nel registro dell'app"
    grep -E "FATAL EXCEPTION|AndroidRuntime|dev.pages.mywinecellar" "$OUT/logcat.txt" | head -60
  fi
} > "$OUT/summary.md" 2>&1

git config --global user.name "cantina-ci"
git config --global user.email "ci@users.noreply.github.com"
rm -rf /tmp/res && mkdir /tmp/res && cp -r "$OUT"/. /tmp/res/
git checkout --orphan ci-results-tmp
git rm -rf . > /dev/null 2>&1
cp -r /tmp/res/. .
git add -A
git commit -m "Risultati CI run $GITHUB_RUN_NUMBER" > /dev/null
git push -f origin HEAD:ci-results
exit 0
