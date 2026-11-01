const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export async function api(path, options = {}, token = localStorage.getItem('cabline-token')) {
  const headers = new Headers(options.headers || {})
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (response.status === 204) return null
  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json') ? await response.json() : await response.text()
  if (!response.ok) throw new Error(payload?.message || payload || `Request failed (${response.status})`)
  return payload
}

export const post = (path, body, token) => api(path, { method: 'POST', body: JSON.stringify(body) }, token)
export const patch = (path, body, token) => api(path, { method: 'PATCH', body: JSON.stringify(body) }, token)

export function authCallbackToken() {
  const hash = new URLSearchParams(window.location.hash.slice(1))
  const token = hash.get('accessToken')
  if (token) {
    localStorage.setItem('cabline-token', token)
    history.replaceState(null, '', window.location.pathname + window.location.search)
  }
  return token
}
