// Supabase Edge Function: customer-auth  (versione 2: accesso con codice via email)
//
// COMPATIBILE CON LA VERSIONE ATTUALE: finché non attivi LOGIN_MODE=code, le azioni "link" e "login"
// funzionano esattamente come prima. Con LOGIN_MODE=code serve il codice di 6 cifre inviato via email.
//
// Secret usati (Edge Functions → Secrets):
//   CUSTOMER_STANDARD_PASSWORD  (già presente)
//   LOGIN_MODE                  "legacy" (predefinito) oppure "code"
//   EMAILJS_PRIVATE_KEY         (già presente per submit-request)
//   EMAILJS_CODE_TEMPLATE_ID    id del modello EmailJS per il codice (vedi la guida)
//   CODE_SECRET                 facoltativo: chiave per proteggere i codici (se manca si usa SUPABASE_SERVICE_ROLE_KEY)
// Facoltativi: EMAILJS_PUBLIC_KEY, EMAILJS_SERVICE_ID (stessi valori di submit-request).
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Content-Type': 'application/json',
}

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: corsHeaders })

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!
const SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const CUSTOMER_STANDARD_PASSWORD = Deno.env.get('CUSTOMER_STANDARD_PASSWORD')
const LOGIN_MODE: 'legacy' | 'code' =
  (Deno.env.get('LOGIN_MODE') || 'legacy').trim().toLowerCase() === 'code' ? 'code' : 'legacy'
const CODE_SECRET = Deno.env.get('CODE_SECRET') || SERVICE_ROLE_KEY

const EMAILJS_PRIVATE_KEY = Deno.env.get('EMAILJS_PRIVATE_KEY')
const EMAILJS_PUBLIC_KEY = Deno.env.get('EMAILJS_PUBLIC_KEY') || 'Xhp3dLoLUQglcf5oO'
const EMAILJS_SERVICE_ID = Deno.env.get('EMAILJS_SERVICE_ID') || 'service_dhpzjg4'
const EMAILJS_CODE_TEMPLATE_ID = Deno.env.get('EMAILJS_CODE_TEMPLATE_ID')

const CODE_TTL_MINUTES = 10
const MAX_ATTEMPTS = 5
const MAX_CODES_PER_EMAIL_PER_HOUR = 3
const MAX_CODES_PER_IP_PER_HOUR = 10

if (!CUSTOMER_STANDARD_PASSWORD) {
  console.error('Missing CUSTOMER_STANDARD_PASSWORD secret')
}

const admin = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
  auth: { autoRefreshToken: false, persistSession: false },
})

function validEmail(email: unknown): email is string {
  return typeof email === 'string' && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

function getBearerToken(req: Request): string | null {
  const header = req.headers.get('Authorization') || ''
  return header.startsWith('Bearer ') ? header.slice(7).trim() : null
}

async function getAuthenticatedUser(req: Request) {
  const token = getBearerToken(req)
  if (!token) return null
  const { data, error } = await admin.auth.getUser(token)
  if (error || !data.user) return null
  return data.user
}

function clientIp(req: Request): string {
  return req.headers.get('cf-connecting-ip') ||
    (req.headers.get('x-forwarded-for') || '').split(',')[0].trim() || 'unknown'
}

// ---------------------------------------------------------------- codici

async function hmacHex(key: string, message: string): Promise<string> {
  const enc = new TextEncoder()
  const k = await crypto.subtle.importKey('raw', enc.encode(key), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign'])
  const sig = await crypto.subtle.sign('HMAC', k, enc.encode(message))
  return Array.from(new Uint8Array(sig)).map((b) => b.toString(16).padStart(2, '0')).join('')
}

function safeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false
  let diff = 0
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i)
  return diff === 0
}

function generateCode(): string {
  // 6 cifre, scelte con generatore casuale sicuro (scartando i valori che sbilancerebbero la distribuzione)
  const limit = Math.floor(0x100000000 / 1_000_000) * 1_000_000
  const buf = new Uint32Array(1)
  do {
    crypto.getRandomValues(buf)
  } while (buf[0] >= limit)
  return String(buf[0] % 1_000_000).padStart(6, '0')
}

async function sendCodeEmail(email: string, code: string): Promise<boolean> {
  if (!EMAILJS_PRIVATE_KEY || !EMAILJS_CODE_TEMPLATE_ID) {
    console.error('EmailJS non configurato per i codici (manca EMAILJS_PRIVATE_KEY o EMAILJS_CODE_TEMPLATE_ID)')
    return false
  }
  try {
    const res = await fetch('https://api.emailjs.com/api/v1.0/email/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        service_id: EMAILJS_SERVICE_ID,
        template_id: EMAILJS_CODE_TEMPLATE_ID,
        user_id: EMAILJS_PUBLIC_KEY,
        accessToken: EMAILJS_PRIVATE_KEY,
        template_params: { to_email: email, code, minutes: CODE_TTL_MINUTES, site_name: 'La Cantina di Egon' },
      }),
    })
    if (!res.ok) {
      console.error('EmailJS ha rifiutato l\u2019invio del codice. Stato:', res.status, await res.text())
      return false
    }
    return true
  } catch (e) {
    console.error('Invio del codice non riuscito:', e)
    return false
  }
}

// ---------------------------------------------------------------- operazioni sull'account

// Collega l'email alla sessione anonima del chiamante. L'UUID dell'utente resta invariato.
// email_confirm:true evita la mail di conferma di Supabase (il controllo dell'email è già nel codice).
async function doLink(user: { id: string }, email: string): Promise<Response> {
  const { data, error } = await admin.auth.admin.updateUserById(user.id, {
    email,
    password: CUSTOMER_STANDARD_PASSWORD!,
    email_confirm: true,
  })
  if (error) {
    const message = /already|exists|registered|duplicate/i.test(error.message)
      ? 'Questa email è già associata a un account. Usa “Hai già un account? Accedi”.'
      : error.message
    return json({ ok: false, error: message }, 400)
  }
  return json({ ok: true, user: data.user, message: 'Cantina salvata.' })
}

async function doLogin(email: string): Promise<Response> {
  // La password esiste solo nel secret della funzione.
  const authClient = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
    auth: { autoRefreshToken: false, persistSession: false },
  })
  const { data, error } = await authClient.auth.signInWithPassword({
    email,
    password: CUSTOMER_STANDARD_PASSWORD!,
  })
  if (error || !data.session) {
    return json({ ok: false, error: error?.message || 'Accesso non riuscito.' }, 401)
  }
  return json({ ok: true, session: data.session })
}

// ---------------------------------------------------------------- gestore

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })
  if (req.method !== 'POST') return json({ ok: false, error: 'Metodo non consentito.' }, 405)

  if (!CUSTOMER_STANDARD_PASSWORD) {
    return json({ ok: false, error: 'Configurazione server incompleta.' }, 500)
  }

  let body: { action?: string; email?: string; code?: string; purpose?: string }
  try {
    body = await req.json()
  } catch {
    return json({ ok: false, error: 'Richiesta non valida.' }, 400)
  }

  const action = body.action

  // I client chiedono come devono comportarsi: "legacy" = solo email, "code" = serve il codice via email.
  if (action === 'mode') return json({ ok: true, mode: LOGIN_MODE })

  const email = typeof body.email === 'string' ? body.email.trim().toLowerCase() : ''
  if (!validEmail(email)) return json({ ok: false, error: 'Inserisci un indirizzo email valido.' }, 400)

  try {
    // ---- vecchie azioni: valide solo finché il codice non è obbligatorio
    if (action === 'link' || action === 'login') {
      if (LOGIN_MODE === 'code') {
        return json(
          { ok: false, code: 'code_required', error: 'Per accedere serve il codice inviato via email. Aggiorna l\u2019app o ricarica il sito.' },
          403,
        )
      }
      if (action === 'link') {
        const user = await getAuthenticatedUser(req)
        if (!user) return json({ ok: false, error: 'Sessione non valida.' }, 401)
        if (user.is_anonymous !== true) {
          return json({ ok: false, error: 'Questa sessione non è una sessione cliente anonima.' }, 400)
        }
        return await doLink(user, email)
      }
      return await doLogin(email)
    }

    // ---- richiesta del codice
    if (action === 'request_code') {
      if (LOGIN_MODE !== 'code') return json({ ok: true, mode: 'legacy' })
      const user = await getAuthenticatedUser(req)
      if (!user) return json({ ok: false, error: 'Sessione non valida.' }, 401)
      const purpose = body.purpose === 'link' ? 'link' : 'login'
      if (purpose === 'link' && user.is_anonymous !== true) {
        return json({ ok: false, error: 'Questa sessione non è una sessione cliente anonima.' }, 400)
      }

      const now = Date.now()
      const hourAgo = new Date(now - 3600_000).toISOString()
      await admin.from('login_codes').delete().lt('created_at', new Date(now - 24 * 3600_000).toISOString())

      const { count: byEmail } = await admin.from('login_codes')
        .select('id', { count: 'exact', head: true }).eq('email', email).gte('created_at', hourAgo)
      const ip = clientIp(req)
      const { count: byIp } = await admin.from('login_codes')
        .select('id', { count: 'exact', head: true }).eq('ip', ip).gte('created_at', hourAgo)
      if ((byEmail ?? 0) >= MAX_CODES_PER_EMAIL_PER_HOUR || (byIp ?? 0) >= MAX_CODES_PER_IP_PER_HOUR) {
        return json({ ok: false, error: 'Hai richiesto troppi codici. Riprova tra un po\u2019.' }, 429)
      }

      // un solo codice valido alla volta per email e scopo
      await admin.from('login_codes').update({ used: true }).eq('email', email).eq('purpose', purpose).eq('used', false)

      const code = generateCode()
      const codeHash = await hmacHex(CODE_SECRET, `${email}|${purpose}|${code}`)
      const { error: insErr } = await admin.from('login_codes').insert({
        email, purpose, code_hash: codeHash, ip,
        expires_at: new Date(now + CODE_TTL_MINUTES * 60_000).toISOString(),
      })
      if (insErr) throw insErr

      // La risposta è identica sia che l'email abbia già un account sia che no (niente enumerazione).
      const sent = await sendCodeEmail(email, code)
      if (!sent) {
        return json({ ok: false, error: 'Non riesco a inviare l\u2019email in questo momento. Riprova più tardi.' }, 502)
      }
      return json({ ok: true, mode: 'code', expiresInMinutes: CODE_TTL_MINUTES })
    }

    // ---- verifica del codice
    if (action === 'verify_code') {
      if (LOGIN_MODE !== 'code') return json({ ok: false, error: 'Accesso con codice non attivo.' }, 400)
      const user = await getAuthenticatedUser(req)
      if (!user) return json({ ok: false, error: 'Sessione non valida.' }, 401)
      const purpose = body.purpose === 'link' ? 'link' : 'login'
      const code = String(body.code ?? '').replace(/\D/g, '')
      if (code.length !== 6) return json({ ok: false, error: 'Inserisci il codice di 6 cifre.' }, 400)
      if (purpose === 'link' && user.is_anonymous !== true) {
        return json({ ok: false, error: 'Questa sessione non è una sessione cliente anonima.' }, 400)
      }

      const { data: row } = await admin.from('login_codes').select('*')
        .eq('email', email).eq('purpose', purpose).eq('used', false)
        .gt('expires_at', new Date().toISOString())
        .order('created_at', { ascending: false }).limit(1).maybeSingle()
      if (!row) return json({ ok: false, error: 'Codice scaduto o non valido. Richiedine uno nuovo.' }, 400)

      if ((row.attempts ?? 0) >= MAX_ATTEMPTS) {
        await admin.from('login_codes').update({ used: true }).eq('id', row.id)
        return json({ ok: false, error: 'Troppi tentativi. Richiedi un nuovo codice.' }, 429)
      }

      const expected = await hmacHex(CODE_SECRET, `${email}|${purpose}|${code}`)
      if (!safeEqual(expected, row.code_hash)) {
        const attempts = (row.attempts ?? 0) + 1
        await admin.from('login_codes').update({ attempts, used: attempts >= MAX_ATTEMPTS }).eq('id', row.id)
        return json({ ok: false, error: 'Codice non corretto.' }, 400)
      }

      await admin.from('login_codes').update({ used: true }).eq('id', row.id)
      if (purpose === 'login') return await doLogin(email)

      // "link": appena l'email è collegata Supabase annulla la sessione da ospite, quindi restituiamo
      // subito una sessione nuova (stesso utente, stesso id) con cui il client può continuare.
      const linked = await doLink(user, email)
      if (linked.status !== 200) return linked
      const linkedBody = await linked.json()
      const login = await doLogin(email)
      const loginBody = await login.json()
      if (!loginBody.ok) return json(loginBody, login.status)
      return json({ ok: true, user: linkedBody.user, message: linkedBody.message, session: loginBody.session })
    }

    return json({ ok: false, error: 'Azione non riconosciuta.' }, 400)
  } catch (error) {
    console.error('customer-auth error:', error)
    return json({ ok: false, error: 'Errore interno del server.' }, 500)
  }
})
