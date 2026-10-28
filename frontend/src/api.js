const API = '/api'

export async function request(path, options = {}) {
  const token = localStorage.getItem('taller-token')
  const headers = { ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers }
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers,
  })
  const body = await response.json().catch(() => ({}))
  if (response.status === 401 && !path.startsWith('/auth/')) {
    clearSession()
    window.location.assign('/')
  }
  if (!response.ok) throw new Error(body.message || body.detail || 'No se pudo completar la solicitud. Inténtalo de nuevo.')
  return body
}

/** Realiza una petición autenticada que devuelve bytes de imagen en lugar de JSON. */
export async function request_blob(path) {
  const token = localStorage.getItem('taller-token')
  const response = await fetch(`${API}${path}`, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    if (response.status === 401) {
      clearSession()
      window.location.assign('/')
    }
    throw new Error(error.message || error.detail || 'No se pudo cargar la fotografía.')
  }
  return response.blob()
}

export function saveSession(data) {
  localStorage.setItem('taller-token', data.token)
  localStorage.setItem('taller-user', JSON.stringify(data.user))
}
export function clearSession() { localStorage.removeItem('taller-token'); localStorage.removeItem('taller-user') }

/** Lee los roles firmados del JWT y mantiene el frontend alineado con la política del backend. */
export function hasAnyRole(expectedRoles = []) {
  try {
    const token = localStorage.getItem('taller-token')
    const payload = token?.split('.')[1]?.replace(/-/g, '+').replace(/_/g, '/')
    if (!payload) return false
    const claims = JSON.parse(atob(payload.padEnd(Math.ceil(payload.length / 4) * 4, '=')))
    const granted = Array.isArray(claims.roles) ? claims.roles.map(role => String(role).replace(/^ROLE_/, '').toUpperCase()) : []
    return expectedRoles.some(role => granted.includes(role.toUpperCase()))
  } catch {
    return false
  }
}
