import { test, beforeEach } from 'node:test'
import assert from 'node:assert/strict'
import { transform } from 'esbuild'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import * as mock from './mock-supabase.mjs'

const here = path.dirname(fileURLToPath(import.meta.url))
const source = fs.readFileSync(path.join(here, '..', 'customer-auth.ts'), 'utf8')
  .replace("'https://esm.sh/@supabase/supabase-js@2'", JSON.stringify(pathToFileURL(path.join(here, 'mock-supabase.mjs')).href))
const { code: js } = await transform(source, { loader: 'ts', format: 'esm' })
const tmp = path.join(here, '.tmp'); fs.mkdirSync(tmp, { recursive: true })

let counter = 0
const sentEmails = []
globalThis.fetch = async (url, init) => {
  if (String(url).includes('emailjs')) { sentEmails.push(JSON.parse(init.body)); return { ok: true, status: 200, text: async () => 'OK' } }
  throw new Error('rete non prevista: ' + url)
}

// Carica la funzione con un certo "ambiente" (secret) e restituisce il suo gestore
async function load(env) {
  const vars = { SUPABASE_URL: 'http://x', SUPABASE_SERVICE_ROLE_KEY: 'service-key', CUSTOMER_STANDARD_PASSWORD: 'std-pass',
    EMAILJS_PRIVATE_KEY: 'pk', EMAILJS_CODE_TEMPLATE_ID: 'template_code', ...env }
  globalThis.Deno = { env: { get: (k) => vars[k] }, serve: (h) => { globalThis.__handler = h } }
  const file = path.join(tmp, `fn-${++counter}.mjs`)
  fs.writeFileSync(file, js)
  await import(pathToFileURL(file).href)
  return globalThis.__handler
}

let handler, ipCounter
async function call(body, { token, ip = '1.1.1.1' } = {}) {
  const headers = { 'Content-Type': 'application/json', 'x-forwarded-for': ip }
  if (token) headers.Authorization = 'Bearer ' + token
  const res = await handler(new Request('http://f/', { method: 'POST', headers, body: JSON.stringify(body) }))
  return { status: res.status, body: await res.json() }
}
function addUser(id, email, anonymous = false) {
  mock.users.set(id, { id, email, password: anonymous ? '' : 'std-pass', is_anonymous: anonymous })
  mock.tokens.set('T-' + id, id)
  return 'T-' + id
}
const lastCode = () => sentEmails.at(-1).template_params.code

beforeEach(() => { mock.reset(); sentEmails.length = 0 })

// ------------------------------------------------------------------ modalità "legacy" (come oggi)
test('legacy: mode = legacy, request_code non fa nulla, login e link funzionano come prima', async () => {
  handler = await load({})
  assert.deepEqual((await call({ action: 'mode' })).body, { ok: true, mode: 'legacy' })
  assert.deepEqual((await call({ action: 'request_code', email: 'a@b.it' })).body, { ok: true, mode: 'legacy' })
  assert.equal(sentEmails.length, 0)
  addUser('u1', 'cliente@b.it')
  const login = await call({ action: 'login', email: 'cliente@b.it' })
  assert.equal(login.body.ok, true)
  assert.equal(login.body.session.access_token, 'S-u1')
  const tk = addUser('anon1', '', true)
  const link = await call({ action: 'link', email: 'nuovo@b.it' }, { token: tk })
  assert.equal(link.body.ok, true)
  assert.equal(mock.users.get('anon1').email, 'nuovo@b.it')
})

// ------------------------------------------------------------------ modalità "code"
test('code: le vecchie azioni sono bloccate', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  assert.deepEqual((await call({ action: 'mode' })).body, { ok: true, mode: 'code' })
  addUser('u1', 'cliente@b.it')
  const r = await call({ action: 'login', email: 'cliente@b.it' })
  assert.equal(r.status, 403)
  assert.equal(r.body.code, 'code_required')
})

test('code: serve una sessione valida per chiedere il codice', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  assert.equal((await call({ action: 'request_code', email: 'a@b.it' })).status, 401)
  assert.equal(sentEmails.length, 0)
})

test('code: accesso completo con il codice ricevuto, e il codice non si riusa', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  addUser('u1', 'cliente@b.it')
  const tk = addUser('anon1', '', true)
  const req = await call({ action: 'request_code', email: 'Cliente@B.it ', purpose: 'login' }, { token: tk })
  assert.equal(req.body.ok, true)
  assert.equal(JSON.stringify(req.body).includes(lastCode()), false, 'il codice non deve stare nella risposta')
  assert.match(lastCode(), /^\d{6}$/)
  assert.equal(sentEmails.at(-1).template_params.to_email, 'cliente@b.it')
  // nel database c'è solo l'impronta, non il codice
  assert.equal(JSON.stringify(mock.db.login_codes).includes(lastCode()), false)
  const ok = await call({ action: 'verify_code', email: 'cliente@b.it', code: lastCode(), purpose: 'login' }, { token: tk })
  assert.equal(ok.body.ok, true)
  assert.equal(ok.body.session.access_token, 'S-u1')
  const again = await call({ action: 'verify_code', email: 'cliente@b.it', code: lastCode(), purpose: 'login' }, { token: tk })
  assert.equal(again.status, 400)
})

test('code: codice sbagliato, blocco dopo 5 tentativi anche con il codice giusto', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  addUser('u1', 'cliente@b.it'); const tk = addUser('anon1', '', true)
  await call({ action: 'request_code', email: 'cliente@b.it' }, { token: tk })
  const right = lastCode()
  const wrong = right === '000000' ? '111111' : '000000'
  for (let i = 0; i < 5; i++) {
    const r = await call({ action: 'verify_code', email: 'cliente@b.it', code: wrong }, { token: tk })
    assert.equal(r.status, 400); assert.equal(r.body.ok, false)
  }
  const late = await call({ action: 'verify_code', email: 'cliente@b.it', code: right }, { token: tk })
  assert.equal(late.body.ok, false, 'dopo 5 errori il codice non vale più')
})

test('code: il codice scade dopo 10 minuti', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  addUser('u1', 'cliente@b.it'); const tk = addUser('anon1', '', true)
  await call({ action: 'request_code', email: 'cliente@b.it' }, { token: tk })
  mock.db.login_codes[0].expires_at = new Date(Date.now() - 1000).toISOString()
  const r = await call({ action: 'verify_code', email: 'cliente@b.it', code: lastCode() }, { token: tk })
  assert.equal(r.body.ok, false)
  assert.match(r.body.error, /scaduto/)
})

test('code: un nuovo codice annulla quello precedente', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  addUser('u1', 'cliente@b.it'); const tk = addUser('anon1', '', true)
  await call({ action: 'request_code', email: 'cliente@b.it' }, { token: tk })
  const first = lastCode()
  await call({ action: 'request_code', email: 'cliente@b.it' }, { token: tk })
  const second = lastCode()
  if (first !== second) {
    assert.equal((await call({ action: 'verify_code', email: 'cliente@b.it', code: first }, { token: tk })).body.ok, false)
  }
  assert.equal((await call({ action: 'verify_code', email: 'cliente@b.it', code: second }, { token: tk })).body.ok, true)
})

test('code: limite di 3 codici all\'ora per email e di 10 per indirizzo IP', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  const tk = addUser('anon1', '', true)
  for (let i = 0; i < 3; i++) assert.equal((await call({ action: 'request_code', email: 'x@b.it' }, { token: tk })).status, 200)
  assert.equal((await call({ action: 'request_code', email: 'x@b.it' }, { token: tk })).status, 429)
  // altre email dallo stesso IP: fino a 10 in totale
  for (let i = 0; i < 7; i++) assert.equal((await call({ action: 'request_code', email: `u${i}@b.it` }, { token: tk })).status, 200)
  assert.equal((await call({ action: 'request_code', email: 'last@b.it' }, { token: tk })).status, 429)
  // un altro IP non è bloccato
  assert.equal((await call({ action: 'request_code', email: 'last@b.it' }, { token: tk, ip: '9.9.9.9' })).status, 200)
})

test('code: la risposta è uguale per email con account e senza (niente enumerazione)', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  addUser('u1', 'esiste@b.it'); const tk = addUser('anon1', '', true)
  const a = await call({ action: 'request_code', email: 'esiste@b.it' }, { token: tk })
  const b = await call({ action: 'request_code', email: 'nonesiste@b.it' }, { token: tk })
  assert.deepEqual(a, b)
})

test('code: un codice chiesto per "login" non vale per "link"', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  const tk = addUser('anon1', '', true)
  await call({ action: 'request_code', email: 'nuovo@b.it', purpose: 'login' }, { token: tk })
  const r = await call({ action: 'verify_code', email: 'nuovo@b.it', code: lastCode(), purpose: 'link' }, { token: tk })
  assert.equal(r.body.ok, false)
  assert.equal(mock.users.get('anon1').email, '')
})

test('code: salvare la cantina collega l\'email solo dopo il codice', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  const tk = addUser('anon1', '', true)
  await call({ action: 'request_code', email: 'nuovo@b.it', purpose: 'link' }, { token: tk })
  assert.equal(mock.users.get('anon1').email, '', 'prima del codice l\'email non è collegata')
  const r = await call({ action: 'verify_code', email: 'nuovo@b.it', code: lastCode(), purpose: 'link' }, { token: tk })
  assert.equal(r.body.ok, true)
  assert.equal(mock.users.get('anon1').email, 'nuovo@b.it')
  assert.equal(r.body.session.access_token, 'S-anon1', 'dopo il collegamento arriva una sessione nuova per lo stesso utente')
})

test('code: email già registrata nel collegamento = messaggio chiaro; account non anonimo rifiutato', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  addUser('u1', 'presa@b.it'); const tk = addUser('anon1', '', true)
  await call({ action: 'request_code', email: 'presa@b.it', purpose: 'link' }, { token: tk })
  const dup = await call({ action: 'verify_code', email: 'presa@b.it', code: lastCode(), purpose: 'link' }, { token: tk })
  assert.equal(dup.body.ok, false); assert.match(dup.body.error, /già associata/)
  const notAnon = await call({ action: 'request_code', email: 'x@b.it', purpose: 'link' }, { token: 'T-u1' })
  assert.equal(notAnon.status, 400)
})

test('code: login con email senza account dopo il codice corretto = errore generico', async () => {
  handler = await load({ LOGIN_MODE: 'code' })
  const tk = addUser('anon1', '', true)
  await call({ action: 'request_code', email: 'nessuno@b.it' }, { token: tk })
  const r = await call({ action: 'verify_code', email: 'nessuno@b.it', code: lastCode() }, { token: tk })
  assert.equal(r.status, 401)
})

test('code: se l\'email non parte, l\'errore è chiaro e nessun codice resta in giro', async () => {
  handler = await load({ LOGIN_MODE: 'code', EMAILJS_CODE_TEMPLATE_ID: '' })
  const tk = addUser('anon1', '', true)
  const r = await call({ action: 'request_code', email: 'a@b.it' }, { token: tk })
  assert.equal(r.status, 502)
})
