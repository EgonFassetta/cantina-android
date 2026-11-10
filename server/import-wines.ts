// COPIA di riferimento della Edge Function «import-wines» pubblicata su Supabase (progetto xqhoznbjyoityjtrolzb),
// incollata da Egon il 10/11/2026. NON viene pubblicata da qui: serve per sapere cosa fa il server.
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

// Importazione Excel nella cantina DELL'ADMIN che chiama. La cantina si ricava dalla sua utenza:
// «Aggiungi» aggiunge alla sua cantina; «Cancella tutto e sostituisci» sostituisce SOLO i suoi vini.
// Nessun Admin può toccare le cantine degli altri.

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Content-Type': 'application/json',
}
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: corsHeaders })

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!
const SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const ADMIN_EMAIL = Deno.env.get('ADMIN_EMAIL') || 'admin.winecellar@aol.it'
const DEFAULT_TENANT = Deno.env.get('DEFAULT_TENANT') || 'egon'   // deve coincidere con default_tenant() nel database
const admin = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, { auth: { autoRefreshToken: false, persistSession: false } })

async function adminInfo(email: string | null | undefined): Promise<{ isAdmin: boolean; tenant: string | null }> {
  const mail = String(email ?? '').toLowerCase()
  if (!mail) return { isAdmin: false, tenant: null }
  try {
    const { data } = await admin.from('admin_users').select('role,cantina_slug').eq('email', mail).maybeSingle()
    if (data?.role) return { isAdmin: true, tenant: data.cantina_slug ? String(data.cantina_slug).toLowerCase() : (mail === ADMIN_EMAIL.toLowerCase() ? DEFAULT_TENANT : null) }
  } catch (_) { /* tabella non disponibile */ }
  return mail === ADMIN_EMAIL.toLowerCase() ? { isAdmin: true, tenant: DEFAULT_TENANT } : { isAdmin: false, tenant: null }
}

// Solo queste colonne vengono scritte in «wines»: il resto (id, cantina di appartenenza, campi sconosciuti) viene scartato.
// Il «prezzo acquisto» è un dato riservato: non sta in «wines» ma nella tabella «wine_private».
const COLUMNS = ['cantina', 'tipo', 'nome', 'annata', 'quantita', 'posizione', 'dimensione', 'formato', 'grado', 'colore_vino', 'effervescenza', 'dolcezza',
  'paese', 'regione', 'prezzo', 'valore mercato', 'disponibile_vendita', 'escluso_vetrina', 'preferito', 'bevuto', 'data_apertura', 'data_fine',
  'giudizio_complessivo', 'giudizio_bevitore', 'morbidezza', 'struttura', 'tannicita', 'acidita', 'mineralita', 'nota', 'sito_produttore', 'immagine_url']
const genId = () => (typeof crypto.randomUUID === 'function') ? crypto.randomUUID() : 'w' + Date.now() + Math.random().toString(36).slice(2)
const chunks = <T>(a: T[], n: number): T[][] => { const o: T[][] = []; for (let i = 0; i < a.length; i += n) o.push(a.slice(i, i + n)); return o }

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })
  if (req.method !== 'POST') return json({ ok: false, error: 'Metodo non consentito.' }, 405)

  const token = (req.headers.get('Authorization') || '').replace(/^Bearer\s+/i, '').trim()
  const { data: ud, error: ue } = await admin.auth.getUser(token)
  if (ue || !ud?.user) return json({ ok: false, error: 'Sessione non valida.' }, 401)
  const info = await adminInfo(ud.user.email)
  if (!info.isAdmin) return json({ ok: false, error: 'Accesso negato: solo gli amministratori possono importare dati.' }, 403)
  if (!info.tenant) return json({ ok: false, error: 'Al tuo account non è ancora assegnata una cantina: chiedilo al Super Admin.' }, 403)
  const tenant = info.tenant

  let body: any
  try { body = await req.json() } catch { return json({ ok: false, error: 'Richiesta non valida.' }, 400) }
  const rows = Array.isArray(body?.rows) ? body.rows : []
  const replace = !!body?.replace
  if (!rows.length) return json({ ok: false, error: 'Nessuna riga da importare.' }, 400)
  if (rows.length > 5000) return json({ ok: false, error: 'Troppe righe in una sola importazione (massimo 5000).' }, 400)

  try {
    const privates: { wine_id: string; prezzo_acquisto: number }[] = []
    const toInsert = rows.filter((r: unknown) => r && typeof r === 'object').map((r: Record<string, unknown>) => {
      const clean: Record<string, unknown> = {}
      for (const c of COLUMNS) if (c in r) clean[c] = r[c]
      const id = genId()
      const pa = r['prezzo acquisto'] === null || r['prezzo acquisto'] === undefined || r['prezzo acquisto'] === '' ? NaN : Number(String(r['prezzo acquisto']).replace(',', '.'))
      if (Number.isFinite(pa) && pa >= 0) privates.push({ wine_id: id, prezzo_acquisto: pa })
      return { ...clean, id, tenant }
    })
    if (!toInsert.length) return json({ ok: false, error: 'Nessuna riga valida da importare.' }, 400)

    // Sostituzione sicura: prima si inseriscono i vini nuovi, poi si eliminano i vecchi della STESSA cantina.
    // Se l'inserimento fallisce, i vini nuovi già inseriti vengono tolti e quelli vecchi restano intatti.
    let oldIds: string[] = []
    if (replace) {
      const { data: old, error: oErr } = await admin.from('wines').select('id').eq('tenant', tenant)
      if (oErr) throw oErr
      oldIds = (old ?? []).map((w: { id: string }) => String(w.id))
    }
    const inserted: string[] = []
    for (const chunk of chunks(toInsert, 200)) {
      const { error: insErr } = await admin.from('wines').insert(chunk)
      if (insErr) {
        for (const ids of chunks(inserted, 200)) await admin.from('wines').delete().eq('tenant', tenant).in('id', ids)
        throw insErr
      }
      inserted.push(...chunk.map((c) => String((c as { id: string }).id)))
    }
    for (const ids of chunks(oldIds, 200)) {
      const { error: delErr } = await admin.from('wines').delete().eq('tenant', tenant).in('id', ids)
      if (delErr) throw delErr
      await admin.from('wine_private').delete().in('wine_id', ids)        // i prezzi riservati dei vini sostituiti non restano orfani
    }
    // Prezzi di acquisto (riservati): se non si riesce a salvarli, i vini sono comunque importati e l'esito lo dice.
    let avviso: string | undefined
    for (const chunk of chunks(privates, 200)) {
      const { error: pErr } = await admin.from('wine_private').upsert(chunk, { onConflict: 'wine_id' })
      if (pErr) { avviso = 'Vini importati, ma non sono riuscito a salvare i prezzi di acquisto: ' + pErr.message; break }
    }
    return json({ ok: true, count: toInsert.length, sostituite: oldIds.length, prezziAcquisto: privates.length, ...(avviso ? { avviso } : {}) })
  } catch (error: any) {
    console.error('import-wines error:', error)
    return json({ ok: false, error: error?.message || "Errore durante l'importazione." }, 500)
  }
})
