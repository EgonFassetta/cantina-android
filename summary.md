# Risultati CI — run n. 46

- Commit: 412858c4972e57287f5b30ea3f2e081e2b6fd27e
- Compilazione e test automatici: **success**
- Prova sull'emulatore: **success**
- File per il Play Store (compilazione senza firma): **success**
- Autoverifica della firma (chiave temporanea): **success**
- Funzione customer-auth del server (test): **success**
- Data: 2026-10-29T18:03:58Z

## Test automatici (logica)
test eseguiti: 40, falliti: 0, errori: 0, saltati: 0

## Errori di compilazione

## Ultime righe del log di compilazione
> Task :app:validateSigningCantinaFake
> Task :app:writeCantinaFakeAppMetadata
> Task :app:writeCantinaFakeSigningConfigVersions
> Task :app:processCantinaFakeResources
> Task :app:compileCantinaDebugKotlin
w: file:///home/runner/work/cantina-android/cantina-android/app/src/main/java/dev/pages/mywinecellar/data/Models.kt:31:36 This declaration needs opt-in. Its usage should be marked with '@kotlinx.serialization.ExperimentalSerializationApi' or '@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)'
w: file:///home/runner/work/cantina-android/cantina-android/app/src/main/java/dev/pages/mywinecellar/ui/Screens.kt:226:48 'val Icons.Filled.List: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.List.

> Task :app:compileCantinaFakeKotlin
w: file:///home/runner/work/cantina-android/cantina-android/app/src/main/java/dev/pages/mywinecellar/data/Models.kt:31:36 This declaration needs opt-in. Its usage should be marked with '@kotlinx.serialization.ExperimentalSerializationApi' or '@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)'
w: file:///home/runner/work/cantina-android/cantina-android/app/src/main/java/dev/pages/mywinecellar/ui/Screens.kt:226:48 'val Icons.Filled.List: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.List.

> Task :app:compileCantinaDebugJavaWithJavac
> Task :app:processCantinaDebugJavaRes
> Task :app:bundleCantinaDebugClassesToCompileJar
> Task :app:bundleCantinaDebugClassesToRuntimeJar
> Task :app:dexBuilderCantinaDebug
> Task :app:mergeCantinaDebugGlobalSynthetics FROM-CACHE
> Task :app:compileCantinaFakeJavaWithJavac
> Task :app:mergeProjectDexCantinaDebug
> Task :app:mergeCantinaDebugJavaResource
> Task :app:compileCantinaDebugUnitTestKotlin
> Task :app:dexBuilderCantinaFake
> Task :app:compileCantinaDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processCantinaDebugUnitTestJavaRes
> Task :app:testCantinaDebugUnitTest
> Task :app:mergeCantinaFakeGlobalSynthetics FROM-CACHE
> Task :app:processCantinaFakeJavaRes
> Task :app:mergeProjectDexCantinaFake
> Task :app:mergeCantinaFakeJavaResource
> Task :app:packageCantinaDebug
> Task :app:createCantinaDebugApkListingFileRedirect
> Task :app:assembleCantinaDebug
> Task :app:packageCantinaFake
> Task :app:createCantinaFakeApkListingFileRedirect
> Task :app:assembleCantinaFake
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/build-1793296028642.json

BUILD SUCCESSFUL in 1m 7s
80 actionable tasks: 55 executed, 25 from cache

## File per il Play Store
total 7896
drwxr-xr-x 3 runner runner    4096 Oct 29 17:49 .
drwxr-xr-x 5 runner runner    4096 Oct 29 17:49 ..
-rw-r--r-- 1 runner runner 8068531 Oct 29 17:49 app-cantina-release-unsigned.apk
drwxr-xr-x 4 runner runner    4096 Oct 29 17:49 baselineProfiles
-rw-r--r-- 1 runner runner     761 Oct 29 17:49 output-metadata.json

app/build/outputs/bundle/cantinaRelease/:
total 7564
drwxr-xr-x 2 runner runner    4096 Oct 29 17:49 .
drwxr-xr-x 3 runner runner    4096 Oct 29 17:49 ..
-rw-r--r-- 1 runner runner 7735988 Oct 29 17:49 app-cantina-release.aab

## Funzione customer-auth (accesso con codice)
# tests 14
# pass 14
# fail 0

## Autoverifica della firma
	for: CN=Prova firma
> Task :app:assembleCantinaRelease
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/signing_check-1793296163472.json

BUILD SUCCESSFUL in 14s
59 actionable tasks: 10 executed, 1 from cache, 48 up-to-date
V2 Signer: certificate DN: CN=Prova firma
V2 Signer: certificate SHA-256 digest: ca59269170d42a4c9dcf660b0c68b8862c9b2010431fe3a00a4c9a3b707fa191
V2 Signer: certificate SHA-1 digest: 594c8206b33be791dff296500ed6785896ea8e57
V2 Signer: certificate MD5 digest: c56a653784c66bc1d2a259627710c92a

jar verified.


## Controlli sul pacchetto
File: app/build/outputs/apk/cantina/release/app-cantina-release.apk
package: name='mywine.cellar' versionCode='1' versionName='1.0.0' platformBuildVersionName='16' platformBuildVersionCode='36' compileSdkVersion='36' compileSdkVersionCodename='16'
targetSdkVersion:'36'
uses-permission: name='android.permission.INTERNET'
uses-permission: name='mywine.cellar.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
application-label:'La Cantina di Egon'
native-code: 'arm64-v8a' 'armeabi-v7a' 'x86' 'x86_64'
--- librerie native nel pacchetto:
      1 lib/arm64-v8a
      1 lib/armeabi-v7a
      1 lib/x86
      1 lib/x86_64
--- dimensioni:
8076723 app/build/outputs/apk/cantina/release/app-cantina-release.apk
7745454 app/build/outputs/bundle/cantinaRelease/app-cantina-release.aab

## Rete del computer di GitHub verso il database
database wines: HTTP 200
catalogo: 190 vini | disponibili: 1 | con foto: 0
primo disponibile: Begali - None | foto: False

## Prova sull'emulatore
esito: 0 (0 = ok)

### Passaggi della prova
Performing Streamed Install
Success
Starting: Intent { cmp=mywine.cellar/dev.pages.mywinecellar.MainActivity }
Status: ok
LaunchState: COLD
Activity: mywine.cellar/dev.pages.mywinecellar.MainActivity
TotalTime: 4555
WaitTime: 4780
Complete
== 1) Catalogo
OK: catalogo caricato dal database
== 2) Scheda del vino e preferito (come ospite)
OK: scheda del vino aperta
OK: altri vini della stessa cantina
OK: avviso per gli ospiti
OK: cuore attivo (preferito salvato)
== 3) Elenco preferiti
OK: il vino è tra i preferiti
== 4) Chiudo e riapro l'app: il preferito deve restare
OK: catalogo ricaricato
OK: il preferito è ancora lì dopo la riapertura
OK: preferito rimosso
== 4a) Gruppi per cantina, ricerca e aggiornamento
OK: elenco
OK: intestazione della cantina
vini: prima=190, cercando Amarone=90
OK: la ricerca restringe l'elenco
OK: cancellando la ricerca tornano tutti i vini
OK: l'elenco è ancora lì dopo l'aggiornamento
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
== 6c) Tema scuro e testo ingrandito
OK: catalogo con tema scuro
OK: scheda con tema scuro
OK: catalogo con testo ingrandito
OK: schermata account con testo ingrandito
OK: catalogo di nuovo normale
== 6e) Schermo largo (tablet) e telefono in orizzontale
OK: catalogo su schermo largo
OK: scheda su schermo largo
OK: catalogo in orizzontale
== 6d) Prove con server simulato: accesso con codice, richieste, eliminazione
Success
OK: catalogo dal server simulato
-- bottiglie riservate per le serate (come sul sito)
OK: il vino tutto riservato è mostrato come Riservata
vini: tutti=3, solo disponibili=1
OK: i vini riservati non compaiono tra i disponibili
-- salvare la cantina con il codice
OK: schermata account da ospite
OK: passo del codice
OK: codice sbagliato rifiutato
OK: cantina salvata: sei collegato
OK: il server ha collegato l'email all'account
-- richiesta dal carrello
OK: elenco
OK: scheda con pulsante carrello
OK: carrello con il vino
OK: finestra della richiesta
OK: carrello svuotato dopo l'invio

### Errori gravi nel registro dell'app
