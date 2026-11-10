# Risultati CI — run n. 54

- Commit: 755844e61d830a99d51ed50fb48bb9409dc3e51f
- Compilazione e test automatici: **success**
- Prova sull'emulatore: **success**
- File per il Play Store (compilazione senza firma): **success**
- Autoverifica della firma (chiave temporanea): **success**
- Funzione customer-auth del server (test): **success**
- Data: 2026-11-10T13:33:17Z

## Test automatici (logica)
test eseguiti: 72, falliti: 0, errori: 0, saltati: 0

## Errori di compilazione

## Ultime righe del log di compilazione
> Task :app:validateSigningCantinaFake
> Task :app:writeCantinaFakeAppMetadata
> Task :app:writeCantinaFakeSigningConfigVersions
> Task :app:compileCantinaDebugKotlin
> Task :app:compileCantinaFakeKotlin
w: file:///home/runner/work/cantina-android/cantina-android/app/src/main/java/dev/pages/mywinecellar/data/Models.kt:31:36 This declaration needs opt-in. Its usage should be marked with '@kotlinx.serialization.ExperimentalSerializationApi' or '@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)'
w: file:///home/runner/work/cantina-android/cantina-android/app/src/main/java/dev/pages/mywinecellar/ui/Screens.kt:232:48 'val Icons.Filled.List: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.List.

> Task :app:compileCantinaDebugKotlin
w: file:///home/runner/work/cantina-android/cantina-android/app/src/main/java/dev/pages/mywinecellar/data/Models.kt:31:36 This declaration needs opt-in. Its usage should be marked with '@kotlinx.serialization.ExperimentalSerializationApi' or '@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)'
w: file:///home/runner/work/cantina-android/cantina-android/app/src/main/java/dev/pages/mywinecellar/ui/Screens.kt:232:48 'val Icons.Filled.List: ImageVector' is deprecated. Use the AutoMirrored version at Icons.AutoMirrored.Filled.List.

> Task :app:compileCantinaFakeJavaWithJavac
> Task :app:compileCantinaDebugJavaWithJavac
> Task :app:processCantinaDebugJavaRes
> Task :app:bundleCantinaDebugClassesToRuntimeJar
> Task :app:bundleCantinaDebugClassesToCompileJar
> Task :app:dexBuilderCantinaDebug
> Task :app:mergeCantinaDebugGlobalSynthetics FROM-CACHE
> Task :app:compileCantinaDebugUnitTestKotlin
> Task :app:mergeCantinaDebugJavaResource
> Task :app:mergeProjectDexCantinaDebug
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
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/build-1794315922846.json

BUILD SUCCESSFUL in 1m 20s
80 actionable tasks: 45 executed, 35 from cache

## File per il Play Store
total 8072
drwxr-xr-x 3 runner runner    4096 Nov 10 13:07 .
drwxr-xr-x 5 runner runner    4096 Nov 10 13:07 ..
-rw-r--r-- 1 runner runner 8248755 Nov 10 13:07 app-cantina-release-unsigned.apk
drwxr-xr-x 4 runner runner    4096 Nov 10 13:07 baselineProfiles
-rw-r--r-- 1 runner runner     761 Nov 10 13:07 output-metadata.json

app/build/outputs/bundle/cantinaRelease/:
total 7740
drwxr-xr-x 2 runner runner    4096 Nov 10 13:07 .
drwxr-xr-x 3 runner runner    4096 Nov 10 13:07 ..
-rw-r--r-- 1 runner runner 7917231 Nov 10 13:07 app-cantina-release.aab

## Funzione customer-auth (accesso con codice)
# tests 14
# pass 14
# fail 0

## Autoverifica della firma
	for: CN=Prova firma
> Task :app:bundleCantinaRelease
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/signing_check-1794316078674.json

BUILD SUCCESSFUL in 13s
59 actionable tasks: 8 executed, 3 from cache, 48 up-to-date
V2 Signer: certificate DN: CN=Prova firma
V2 Signer: certificate SHA-256 digest: d330b3003978c39da5c7ce97842f036ea2ea7c5bdca8f470ff504a0219488516
V2 Signer: certificate SHA-1 digest: 02433c23c9859dfe6f63d49f9be9b6665b352974
V2 Signer: certificate MD5 digest: cfbfba4ac194456fabe9aa5f5b59625f

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
8256947 app/build/outputs/apk/cantina/release/app-cantina-release.apk
7926677 app/build/outputs/bundle/cantinaRelease/app-cantina-release.aab

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
TotalTime: 3571
WaitTime: 3612
Complete
== 0) Scelta della cantina (prima apertura, come la prima pagina del sito)
OK: schermata «Scegli una cantina»
OK: elenco delle cantine pubbliche con il pulsante Entra
OK: contatore delle etichette
== 1) Catalogo
OK: catalogo caricato dal database
OK: nome della cantina e «Cambia cantina»
== 1b) Cambia cantina
OK: di nuovo la scelta, con la cantina attuale evidenziata
OK: indietro: si torna alla stessa cantina
OK: rientro nella cantina scelta
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
OK: scelta della cantina (server simulato)
OK: catalogo dal server simulato
-- bottiglie riservate per le serate (come sul sito)
OK: il vino tutto riservato è mostrato come Riservata
vini: tutti=3, solo disponibili=1
OK: i vini riservati non compaiono tra i disponibili
-- salvare la cantina con il codice
OK: schermata account da ospite

### Errori gravi nel registro dell'app
