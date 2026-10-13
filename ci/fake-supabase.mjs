// Server simulato di Supabase, SOLO per le prove automatiche dell'app (variante "fake").
// Riproduce le chiamate usate dall'app: catalogo, sessione anonima, preferiti, carrello,
// richieste, accesso con codice via email, eliminazione account.
// Prove: GET /_state mostra l'ultimo codice, le richieste ricevute e gli utenti; POST /_mode {"mode":"code"|"legacy"}.
import http from 'node:http'
import crypto from 'node:crypto'

const PORT = 8787
let mode = 'code'
const users = new Map()     // id -> { id, email, is_anonymous }
const sessions = new Map()  // access_token -> userId
const refreshes = new Map() // refresh_token -> userId
const state = new Map()     // `${uid}|${wine}` -> { preferito }
const cart = new Map()      // `${uid}|${wine}` -> quantity
const requests = []
const codes = new Map()     // `${email}|${purpose}` -> code
let lastCode = null

const WINES = [
  { id: 'w1', cantina: 'Cantina Prova', nome: 'Rosso Disponibile', tipo: 'Rosso Riserva', annata: 2018, quantita: 3, grado: 14, 'valore mercato': 25.5, disponibile_vendita: true, colore_vino: 'Rosso', effervescenza: 'Fermo', dolcezza: 'Secco', regione: 'Veneto', paese: 'Italia' },
  { id: 'w2', cantina: 'Cantina Prova', nome: 'Bianco Esaurito', annata: 2020, quantita: 0, disponibile_vendita: false, colore_vino: 'Bianco', effervescenza: 'Fermo', dolcezza: 'Secco', regione: 'Friuli-Venezia Giulia', paese: 'Italia' },
  { id: 'w3', cantina: 'Altra Cantina', nome: 'Bollicine', annata: 2021, quantita: 2, disponibile_vendita: true, colore_vino: 'Bianco', effervescenza: 'Bollicine', dolcezza: 'Brut', regione: 'Veneto', paese: 'Italia' },
]

const rnd = () => crypto.randomUUID()
function newSession(user) {
  const at = 'AT-' + rnd(), rt = 'RT-' + rnd()
  sessions.set(at, user.id); refreshes.set(rt, user.id)
  return { access_token: at, refresh_token: rt, user: { id: user.id, email: user.email || '', is_anonymous: !!user.is_anonymous } }
}
// Come sul server vero: dopo il collegamento dell'email il refresh token dell'ospite non vale più
// (il token di accesso resta valido finché non scade, quindi le funzioni lo accettano ancora).
function revokeRefresh(uid) {
  for (const [rt, id] of [...refreshes.entries()]) if (id === uid) refreshes.delete(rt)
}
function send(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json' })
  res.end(body === undefined ? '' : JSON.stringify(body))
}
const q = (u, k) => u.searchParams.get(k)
const eqVal = (v) => (v && v.startsWith('eq.') ? decodeURIComponent(v.slice(3)) : null)

http.createServer(async (req, res) => {
  const u = new URL(req.url, 'http://x')
  const chunks = []
  for await (const c of req) chunks.push(c)
  let body = {}
  try { body = JSON.parse(Buffer.concat(chunks).toString() || '{}') } catch { body = {} }
  const token = (req.headers.authorization || '').replace('Bearer ', '')
  const uid = sessions.get(token)
  const p = u.pathname
  console.log(req.method, p + u.search)

  // ---- strumenti per le prove
  if (p === '/_state') return send(res, 200, { mode, lastCode, requests, users: [...users.values()], cartSize: cart.size, favorites: [...state.values()].filter((s) => s.preferito).length })
  if (p === '/_mode') { mode = body.mode || 'code'; return send(res, 200, { ok: true, mode }) }

  // ---- catalogo
  if (p === '/rest/v1/wines') {
    const offset = parseInt(q(u, 'offset') || '0', 10)
    return send(res, 200, offset === 0 ? WINES : [])
  }

  // ---- sessione
  if (p === '/auth/v1/signup') {
    const user = { id: rnd(), email: '', is_anonymous: true }
    users.set(user.id, user)
    return send(res, 200, newSession(user))
  }
  if (p === '/auth/v1/token') {
    const id = refreshes.get(body.refresh_token)
    const user = id && users.get(id)
    if (!user) return send(res, 400, { error_description: 'Invalid Refresh Token' })
    return send(res, 200, newSession(user))
  }
  if (p === '/auth/v1/logout') return send(res, 204)

  // ---- da qui serve una sessione valida
  if (!uid) return send(res, 401, { message: 'JWT expired' })

  // bottiglie riservate per le serate: il vino w3 (2 bottiglie) è tutto riservato
  if (p === '/rest/v1/rpc/reserved_quantities') return send(res, 200, [{ wine_id: 'w3', riservate: 2 }])

  if (p === '/rest/v1/wine_user_state') {
    const wine = eqVal(q(u, 'wine_id'))
    if (req.method === 'GET') {
      const rows = [...state.entries()].filter(([k, v]) => k.startsWith(uid + '|') && v.preferito).map(([k]) => ({ wine_id: k.split('|')[1] }))
      return send(res, 200, rows)
    }
    if (req.method === 'PATCH') {
      const key = `${uid}|${wine}`
      if (!state.has(key)) return send(res, 200, [])
      state.get(key).preferito = body.preferito
      return send(res, 200, [{ wine_id: wine }])
    }
    if (req.method === 'POST') {
      state.set(`${body.user_id}|${body.wine_id}`, { preferito: body.preferito })
      return send(res, 201)
    }
  }
  if (p === '/rest/v1/wine_user_cart') {
    const wine = eqVal(q(u, 'wine_id'))
    if (req.method === 'GET') {
      return send(res, 200, [...cart.entries()].filter(([k]) => k.startsWith(uid + '|')).map(([k, v]) => ({ wine_id: k.split('|')[1], quantity: v })))
    }
    if (req.method === 'POST') { cart.set(`${body.user_id}|${body.wine_id}`, body.quantity); return send(res, 201) }
    if (req.method === 'DELETE') {
      if (wine) cart.delete(`${uid}|${wine}`)
      else for (const k of [...cart.keys()]) if (k.startsWith(uid + '|')) cart.delete(k)
      return send(res, 204)
    }
  }

  // ---- funzioni
  if (p === '/functions/v1/submit-request') {
    requests.push(body)
    if (JSON.stringify(body).includes('LIMITE')) return send(res, 429, { ok: false, error: 'Hai inviato troppe richieste ravvicinate. Riprova tra un po\u2019.' })
    return send(res, 200, { ok: true })
  }
  if (p === '/functions/v1/delete-account') {
    for (const k of [...state.keys()]) if (k.startsWith(uid + '|')) state.delete(k)
    for (const k of [...cart.keys()]) if (k.startsWith(uid + '|')) cart.delete(k)
    users.delete(uid)
    return send(res, 200, { ok: true })
  }
  if (p === '/functions/v1/customer-auth') {
    const email = String(body.email || '').trim().toLowerCase()
    const purpose = body.purpose === 'link' ? 'link' : 'login'
    if (body.action === 'mode') return send(res, 200, { ok: true, mode })
    if (body.action === 'link' || body.action === 'login') {
      if (mode === 'code') return send(res, 403, { ok: false, code: 'code_required', error: 'Per accedere serve il codice inviato via email.' })
      if (body.action === 'link') { users.get(uid).email = email; revokeRefresh(uid); return send(res, 200, { ok: true }) }
      const found = [...users.values()].find((x) => x.email === email)
      if (!found) return send(res, 401, { ok: false, error: 'Invalid login credentials' })
      return send(res, 200, { ok: true, session: newSession(found) })
    }
    if (body.action === 'request_code') {
      lastCode = String(Math.floor(100000 + Math.random() * 900000))
      codes.set(`${email}|${purpose}`, lastCode)
      return send(res, 200, { ok: true, mode: 'code', expiresInMinutes: 10 })
    }
    if (body.action === 'verify_code') {
      if (codes.get(`${email}|${purpose}`) !== String(body.code)) return send(res, 400, { ok: false, error: 'Codice non corretto.' })
      codes.delete(`${email}|${purpose}`)
      if (purpose === 'link') {
        users.get(uid).email = email; revokeRefresh(uid)
        return send(res, 200, { ok: true, session: newSession(users.get(uid)) })
      }
      let found = [...users.values()].find((x) => x.email === email)
      if (!found) { found = { id: rnd(), email, is_anonymous: false }; users.set(found.id, found) }
      return send(res, 200, { ok: true, session: newSession(found) })
    }
  }
  send(res, 404, { message: 'non previsto dal server simulato: ' + p })
}).listen(PORT, '0.0.0.0', () => console.log('server simulato in ascolto sulla porta', PORT))
