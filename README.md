# La Cantina di Egon — app Android

App Android (Kotlin + Jetpack Compose) del catalogo della cantina. Usa lo stesso database Supabase del sito
https://mywinecellar.pages.dev/ e rispetta le stesse regole di sicurezza: l'app vede solo ciò che vede un cliente del sito.

Identificativo dell'app: `mywine.cellar`.

## Funzioni
Catalogo con ricerca, filtri (colore, caratteristica, tipo vino, regione) e ordinamento · scheda del vino con profilo ·
preferiti · carrello e richiesta · richiesta di disponibilità · assistenza · accesso con email ("Salva la tua cantina") ·
uscita dal dispositivo · eliminazione dell'account · apertura anche senza connessione (ultimo catalogo salvato) ·
aggiornamento tirando giù la lista · gruppi per cantina · altri vini della stessa cantina · condivisione · foto ingrandibile ·
tema chiaro e scuro (segue il telefono) · testo ingrandibile · adattamento ai tablet.
L'area di amministrazione resta sul sito.

## Come viene provata (GitHub Actions)
A ogni modifica il flusso `Android CI`:
1. compila l'app ed esegue i test automatici sulla logica;
2. verifica che il file per il Play Store si compili;
3. avvia un telefono virtuale (Pixel 6, Android 14), percorre le funzioni principali e salva gli screenshot;
4. pubblica un APK di prova nella release **prova**.
I risultati sono nel ramo `ci-results` (`summary.md` e screenshot).

## Pubblicazione
Il flusso `Release` (da avviare a mano) crea il file `.aab` firmato per Google Play. La chiave di firma è nei *Secrets*
dell'archivio (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) e **non** è mai nel codice.

## Prova completa dell'account (spenta)
La prova "salva la cantina → esci → accedi → elimina l'account" è nel file `ci/emulator-test.sh` e parte solo se esiste
il file `ci/account-test.enabled`. Va attivata dopo aver pubblicato la funzione `delete-account` su Supabase, così
l'account di prova viene sempre cancellato a fine test.

## Versioni
`versionCode` e `versionName` sono nel file `app/build.gradle.kts`: il flusso `Release` imposta da solo un `versionCode`
crescente (numero dell'esecuzione), come richiesto da Google Play per ogni nuovo caricamento.
