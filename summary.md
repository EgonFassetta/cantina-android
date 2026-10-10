# Risultati CI — run n. 11

- Commit: c84c5e252a8c7b973d0ccb0e57c2032bb8c03c8b
- Compilazione e test automatici: **success**
- Prova sull'emulatore: **success**
- File per il Play Store (compilazione senza firma): **success**
- Data: 2026-10-10T17:07:33Z

## Test automatici (logica)
test eseguiti: 27, falliti: 0, errori: 0, saltati: 0

## Errori di compilazione

## Ultime righe del log di compilazione
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets
> Task :app:compressDebugAssets FROM-CACHE
> Task :app:desugarDebugFileDependencies FROM-CACHE
> Task :app:mergeDebugJniLibFolders
> Task :app:checkDebugDuplicateClasses
> Task :app:mergeDebugNativeLibs
> Task :app:mergeExtDexDebug FROM-CACHE
> Task :app:mergeLibDexDebug FROM-CACHE

> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so. Run with --info option to learn more.

> Task :app:validateSigningDebug
> Task :app:writeDebugAppMetadata
> Task :app:writeDebugSigningConfigVersions

> Task :app:compileDebugKotlin
w: file:///home/runner/work/cantina-android/cantina-android/app/src/main/java/dev/pages/mywinecellar/data/Models.kt:31:36 This declaration needs opt-in. Its usage should be marked with '@kotlinx.serialization.ExperimentalSerializationApi' or '@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)'

> Task :app:compileDebugJavaWithJavac
> Task :app:bundleDebugClassesToRuntimeJar
> Task :app:processDebugJavaRes
> Task :app:bundleDebugClassesToCompileJar
> Task :app:dexBuilderDebug
> Task :app:mergeDebugGlobalSynthetics FROM-CACHE
> Task :app:mergeProjectDexDebug
> Task :app:mergeDebugJavaResource
> Task :app:compileDebugUnitTestKotlin
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes
> Task :app:testDebugUnitTest
> Task :app:packageDebug
> Task :app:createDebugApkListingFileRedirect
> Task :app:assembleDebug
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/build-1791651648234.json

BUILD SUCCESSFUL in 39s
43 actionable tasks: 25 executed, 18 from cache

## File per il Play Store
total 7712
drwxr-xr-x 3 runner runner    4096 Oct 10 17:02 .
drwxr-xr-x 4 runner runner    4096 Oct 10 17:02 ..
-rw-r--r-- 1 runner runner 7880538 Oct 10 17:02 app-release-unsigned.apk
drwxr-xr-x 4 runner runner    4096 Oct 10 17:02 baselineProfiles
-rw-r--r-- 1 runner runner     730 Oct 10 17:02 output-metadata.json

app/build/outputs/bundle/release/:
total 7384
drwxr-xr-x 2 runner runner    4096 Oct 10 17:02 .
drwxr-xr-x 3 runner runner    4096 Oct 10 17:02 ..
-rw-r--r-- 1 runner runner 7549125 Oct 10 17:02 app-release.aab

## Rete del computer di GitHub verso il database
database wines: HTTP 200

## Prova sull'emulatore
esito: 0 (0 = ok)

### Passaggi della prova
Performing Streamed Install
Success
Starting: Intent { cmp=mywine.cellar/dev.pages.mywinecellar.MainActivity }
Status: ok
LaunchState: COLD
Activity: mywine.cellar/dev.pages.mywinecellar.MainActivity
TotalTime: 4771
WaitTime: 4851
Complete
== 1) Catalogo
OK: catalogo caricato dal database
== 2) Scheda del vino e preferito (come ospite)
OK: scheda del vino aperta
OK: avviso per gli ospiti
OK: cuore attivo (preferito salvato)
== 3) Elenco preferiti
OK: il vino è tra i preferiti
== 4) Chiudo e riapro l'app: il preferito deve restare
OK: catalogo ricaricato
OK: il preferito è ancora lì dopo la riapertura
OK: preferito rimosso
== 4b) Filtri
OK: elenco
OK: finestra dei filtri
OK: filtro attivo
vini: prima=190, con filtro Rosso=187
OK: il filtro per colore restringe l'elenco
OK: filtri azzerati
OK: azzerando i filtri tornano tutti i vini (190)
== 5) Carrello
OK: elenco
OK: scheda con pulsante carrello
OK: aggiunto al carrello
OK: carrello con totale o prezzo da concordare
OK: finestra della richiesta
OK: controllo dei dati obbligatori
OK: carrello svuotato
== 6) Account e assistenza
OK: schermata account da ospite
OK: controllo email
OK: modulo di accesso
OK: finestra assistenza
OK: controllo messaggio
== 7) Apertura senza connessione (mostra l'ultimo catalogo salvato)
OK: avviso di catalogo non aggiornato
OK: i vini si vedono anche offline
OK: nessun crash

### Errori gravi nel registro dell'app
