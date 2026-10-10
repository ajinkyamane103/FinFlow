import axios from 'axios'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
const baseURL = configuredBaseUrl.replace(/\/$/, '')
const TOKEN_KEY = 'finflow_access_token'

const client = axios.create({ baseURL, headers: { 'Content-Type': 'application/json' } })

client.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (token && !config.skipAuth) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !error.config?.skipAuth) {
      window.dispatchEvent(new CustomEvent('finflow:unauthorized'))
    }
    return Promise.reject(error)
  },
)

export const api = {
  register: (payload) => client.post('/auth/register', payload, { skipAuth: true }),
  login: (payload) => client.post('/auth/login', payload, { skipAuth: true }),
  me: () => client.get('/users/me'),
  dashboard: () => client.get('/dashboard'),
  accounts: () => client.get('/accounts'),
  account: (id) => client.get(`/accounts/${id}`),
  createAccount: (payload) => client.post('/accounts', payload),
  deposit: (id, payload, key) => client.post(`/accounts/${id}/deposits`, payload, { headers: { 'Idempotency-Key': key } }),
  withdraw: (id, payload, key) => client.post(`/accounts/${id}/withdrawals`, payload, { headers: { 'Idempotency-Key': key } }),
  transfer: (payload, key) => client.post('/accounts/transfers', payload, { headers: { 'Idempotency-Key': key } }),
  transactions: (id, params) => client.get(`/accounts/${id}/transactions`, { params }),
}

export function errorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data
  if (typeof data === 'string') return data
  return data?.message || data?.error || fallback
}

export { TOKEN_KEY }
