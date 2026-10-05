const fallbackUrl = 'https://sepomex.kurenn.dev'
const baseUrl = (import.meta.env.VITE_SEPOMEX_API_URL || 'http://localhost:3005').replace(/\/$/, '')

async function fetchFrom(url, path) {
  const response = await fetch(`${url}${path}`)
  if (!response.ok) throw new Error('No fue posible consultar el catalogo de direcciones.')
  return response.json()
}

async function request(path) {
  try {
    return await fetchFrom(baseUrl, path)
  } catch (error) {
    if (baseUrl === fallbackUrl) throw error
    return fetchFrom(fallbackUrl, path)
  }
}

export async function findZipCode(zipCode) {
  const payload = await request(`/api/v1/zip_codes?zip_code=${encodeURIComponent(zipCode)}&per_page=200`)
  return payload.zip_codes || []
}

export async function findStates() {
  const payload = await request('/api/v1/states?per_page=200')
  return payload.states || []
}

export async function findMunicipalities(stateId) {
  const payload = await request(`/api/v1/states/${encodeURIComponent(stateId)}/municipalities`)
  return payload.municipalities || []
}

export async function findNeighborhoods(state, municipality) {
  const query = new URLSearchParams({ state, city: municipality, per_page: '200' })
  const payload = await request(`/api/v1/zip_codes?${query.toString()}`)
  return payload.zip_codes || []
}
