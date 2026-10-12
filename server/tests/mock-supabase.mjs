// Mini database e mini servizio di accesso, in memoria, per provare la funzione senza Supabase.
export const db = { login_codes: [], seq: 1 }
export const users = new Map()   // id -> { id, email, password, is_anonymous }
export const tokens = new Map()  // token -> id utente

export function reset() {
  db.login_codes = []; db.seq = 1; users.clear(); tokens.clear()
}

class Query {
  constructor(table) { this.table = table; this.filters = []; this.op = 'select'; this.opts = null }
  select(_cols, opts) { this.opts = opts || null; return this }
  insert(obj) { this.op = 'insert'; this.payload = obj; return this }
  update(obj) { this.op = 'update'; this.payload = obj; return this }
  delete() { this.op = 'delete'; return this }
  eq(c, v) { this.filters.push((r) => r[c] === v); return this }
  gt(c, v) { this.filters.push((r) => r[c] > v); return this }
  gte(c, v) { this.filters.push((r) => r[c] >= v); return this }
  lt(c, v) { this.filters.push((r) => r[c] < v); return this }
  order(c, o) { this.orderBy = [c, o && o.ascending !== false]; return this }
  limit(n) { this.limitN = n; return this }
  maybeSingle() { this.single = true; return this }
  then(res, rej) { return Promise.resolve(this.exec()).then(res, rej) }
  exec() {
    const rows = db[this.table]
    const match = (r) => this.filters.every((f) => f(r))
    if (this.op === 'insert') {
      const row = { id: db.seq++, attempts: 0, used: false, created_at: new Date().toISOString(), ...this.payload }
      rows.push(row)
      return { data: null, error: null }
    }
    if (this.op === 'update') {
      rows.filter(match).forEach((r) => Object.assign(r, this.payload))
      return { data: null, error: null }
    }
    if (this.op === 'delete') {
      db[this.table] = rows.filter((r) => !match(r))
      return { data: null, error: null }
    }
    let out = rows.filter(match)
    if (this.opts && this.opts.count) return { count: out.length, data: null, error: null }
    if (this.orderBy) {
      const [c, asc] = this.orderBy
      out = [...out].sort((a, b) => (a[c] < b[c] ? -1 : a[c] > b[c] ? 1 : 0) * (asc ? 1 : -1))
    }
    if (this.limitN) out = out.slice(0, this.limitN)
    return { data: this.single ? (out[0] ?? null) : out, error: null }
  }
}

export function createClient() {
  return {
    from: (t) => new Query(t),
    auth: {
      getUser: async (token) => {
        const id = tokens.get(token)
        const user = id && users.get(id)
        return user ? { data: { user }, error: null } : { data: { user: null }, error: { message: 'invalid token' } }
      },
      signInWithPassword: async ({ email, password }) => {
        const user = [...users.values()].find((u) => u.email === email && u.password === password)
        if (!user) return { data: {}, error: { message: 'Invalid login credentials' } }
        return { data: { session: { access_token: 'S-' + user.id, refresh_token: 'R-' + user.id, user } }, error: null }
      },
      admin: {
        updateUserById: async (id, attrs) => {
          const dup = [...users.values()].find((u) => u.email === attrs.email && u.id !== id)
          if (dup) return { data: {}, error: { message: 'A user with this email address has already been registered' } }
          const u = users.get(id)
          Object.assign(u, attrs)
          return { data: { user: u }, error: null }
        },
      },
    },
  }
}
