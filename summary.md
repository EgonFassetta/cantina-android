# Risultati CI — run n. 33

- Commit: 18d734dc6c2d650c1b31433c6e35f17dd7b555d9
- Compilazione e test automatici: **success**
- Prova sull'emulatore: **failure**
- File per il Play Store (compilazione senza firma): **success**
- Autoverifica della firma (chiave temporanea): **success**
- Funzione customer-auth del server (test): **success**
- Data: 2026-10-14T19:26:19Z

## Test automatici (logica)
test eseguiti: 77, falliti: 0, errori: 0, saltati: 0

## Errori di compilazione

## Ultime righe del log di compilazione
> Task :app:writeAdminFakeSigningConfigVersions
> Task :app:mergeAdminDebugNativeDebugMetadata NO-SOURCE
> Task :app:mergeAdminDebugShaders
> Task :app:compileAdminDebugShaders NO-SOURCE
> Task :app:generateAdminDebugAssets UP-TO-DATE
> Task :app:mergeAdminFakeNativeLibs
> Task :app:mergeAdminDebugAssets
> Task :app:compressAdminDebugAssets FROM-CACHE

> Task :app:stripAdminFakeDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so. Run with --info option to learn more.

> Task :app:desugarAdminDebugFileDependencies FROM-CACHE
> Task :app:mergeProjectDexAdminFake
> Task :app:dexBuilderAdminDebug
> Task :app:mergeAdminDebugGlobalSynthetics FROM-CACHE
> Task :app:mergeAdminDebugJniLibFolders
> Task :app:checkAdminDebugDuplicateClasses
> Task :app:mergeAdminDebugNativeLibs
> Task :app:mergeExtDexAdminDebug FROM-CACHE
> Task :app:mergeLibDexAdminDebug FROM-CACHE
> Task :app:mergeAdminDebugJavaResource
> Task :app:validateSigningAdminDebug
> Task :app:writeAdminDebugAppMetadata

> Task :app:stripAdminDebugDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so. Run with --info option to learn more.

> Task :app:writeAdminDebugSigningConfigVersions
> Task :app:packageAdminFake
> Task :app:createAdminFakeApkListingFileRedirect
> Task :app:assembleAdminFake
> Task :app:mergeProjectDexAdminDebug
> Task :app:packageAdminDebug
> Task :app:createAdminDebugApkListingFileRedirect
> Task :app:assembleAdminDebug
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/build-1792004729711.json

BUILD SUCCESSFUL in 1m 23s
160 actionable tasks: 122 executed, 38 from cache

## File per il Play Store
total 7732
drwxr-xr-x 3 runner runner    4096 Oct 14 19:07 .
drwxr-xr-x 5 runner runner    4096 Oct 14 19:07 ..
-rw-r--r-- 1 runner runner 7897510 Oct 14 19:07 app-cantina-release-unsigned.apk
drwxr-xr-x 4 runner runner    4096 Oct 14 19:07 baselineProfiles
-rw-r--r-- 1 runner runner     761 Oct 14 19:07 output-metadata.json

app/build/outputs/bundle/cantinaRelease/:
total 7408
drwxr-xr-x 2 runner runner    4096 Oct 14 19:07 .
drwxr-xr-x 3 runner runner    4096 Oct 14 19:07 ..
-rw-r--r-- 1 runner runner 7575014 Oct 14 19:07 app-cantina-release.aab

## Funzione customer-auth (accesso con codice)
# tests 14
# pass 14
# fail 0

## Autoverifica della firma
	for: CN=Prova firma
> Task :app:assembleCantinaRelease
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/signing_check-1792004868620.json

BUILD SUCCESSFUL in 13s
59 actionable tasks: 11 executed, 48 up-to-date
V2 Signer: certificate DN: CN=Prova firma
V2 Signer: certificate SHA-256 digest: b8de1d16e5dd635b34e082bfab6ee50ae018f7353cd105e9e138f7a92c6b30a7
V2 Signer: certificate SHA-1 digest: 0473363d9f2ad63c0abd6ee320a5820bc5b9f58c
V2 Signer: certificate MD5 digest: 527577ad027f4f949418147ee23d161e

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
7905702 app/build/outputs/apk/cantina/release/app-cantina-release.apk
7584172 app/build/outputs/bundle/cantinaRelease/app-cantina-release.aab

## Rete del computer di GitHub verso il database
database wines: HTTP 200
catalogo: 190 vini | disponibili: 1 | con foto: 0
primo disponibile: Begali - None | foto: False

## Prova sull'emulatore
esito: 1 (0 = ok)

### Passaggi della prova
Performing Streamed Install
Success
Starting: Intent { cmp=mywine.cellar/dev.pages.mywinecellar.MainActivity }
Status: ok
LaunchState: COLD
Activity: mywine.cellar/dev.pages.mywinecellar.MainActivity
TotalTime: 4494
WaitTime: 4551
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
