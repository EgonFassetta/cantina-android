# Accesso con codice via email — come attivarlo

## Cosa cambia
- Oggi: chi conosce l'email di un cliente può entrare nella sua cantina.
- Dopo: per "Salva la cantina" e "Accedi" arriva un **codice di 6 cifre via email**, valido 10 minuti. Solo chi legge quella casella può entrare.
- Limiti di sicurezza: 5 tentativi per codice, 3 codici all'ora per email, 10 all'ora per rete. Il codice non viene mai salvato, solo la sua impronta.

## Ordine di attivazione (niente si rompe finché non fai l'ultimo passo)
La nuova funzione si comporta come quella attuale finché non imposti `LOGIN_MODE=code`.

1. **SQL:** esegui `login-codes-setup.sql` nello SQL Editor di Supabase.
2. **EmailJS:** crea il modello del codice (vedi sotto) e annota il suo ID (inizia con `template_`).
3. **Secrets su Supabase** (Edge Functions → Secrets → Add new secret): `EMAILJS_CODE_TEMPLATE_ID` = l'ID del passo 2. Non toccare `LOGIN_MODE` per ora.
4. **Funzione:** Edge Functions → `customer-auth` → Code → incolla tutto il contenuto di `customer-auth.ts` → Deploy.
5. **Pubblica i client aggiornati:** nuovo `index.html` del sito (Cloudflare) e nuovo APK. Funzionano anche con la vecchia modalità.
6. **Prova a freddo:** imposta il secret `LOGIN_MODE` = `code`, apri il sito (o l'app), "Salva la tua cantina" con la tua email: deve arrivare il codice.
7. **Se qualcosa non va:** imposta `LOGIN_MODE` = `legacy` (o cancellalo): torna tutto come prima, subito, senza ripubblicare nulla.

## Il modello su EmailJS (3 minuti)
1. EmailJS → Email Templates → **Create New Template**.
2. **To email:** `{{to_email}}`
3. **Subject:** `Il tuo codice per {{site_name}}`
4. **Contenuto:**
   ```
   Il tuo codice di accesso è: {{code}}

   Vale {{minutes}} minuti. Se non l'hai richiesto tu, ignora questa email.
   ```
5. Salva e copia il **Template ID**.

## Attenzione: limite di email gratuite
Il piano gratuito di EmailJS ha un numero mensile di email limitato (di solito 200). Le email dei codici si sommano a quelle delle richieste dei clienti. Controlla il consumo su EmailJS: se lo superi, i codici smettono di partire e nessuno può accedere. In quel caso rimetti `LOGIN_MODE=legacy`.
