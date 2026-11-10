<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { findMunicipalities, findNeighborhoods, findStates, findZipCode } from './services/sepomex'

const API_BASE = '/api'
const token = ref(localStorage.getItem('auth_token') || '')
const user = ref(null)
const mode = ref('login')
const loading = ref(false)
const error = ref('')
const success = ref('')
const resetToken = ref('')
const roles = ref([])
const activeView = ref('inicio')
const talleres = ref([])
const customersPage = ref({ content: [], number: 0, totalPages: 0, totalElements: 0 })
const customerFilterTallerId = ref('')
const customerSortDir = ref('asc')
const customersLoading = ref(false)
const editingCustomerId = ref(null)
const statuses = ref([])
const statusDraft = reactive({ id: null, strValor: '', strDescripcion: '' })
const vehicleModalCustomer = ref(null)
const vehicleBrands = ref([])
const vehicleModels = ref([])
const vehicleEditModels = ref([])
const vehicleDetail = ref(null)
const vehicleEditModal = ref(null)
const vehicleStatusModal = ref(null)
const selectedVehicleStatusId = ref('')
const confirmVehicleStatusChange = ref(false)
const customerStatusModal = ref(null)
const selectedCustomerStatusId = ref('')
const customerPhotoInput = ref(null)
const customerPhotoPreview = ref('')
const sepomexStates = ref([])
const sepomexMunicipalities = ref([])
const sepomexNeighborhoods = ref([])
const selectedStateId = ref('')
const selectedMunicipalityId = ref('')
const tallerSelectedStateId = ref('')
const tallerSelectedMunicipalityId = ref('')
const tallerMunicipalities = ref([])
const tallerNeighborhoods = ref([])
const tallerAddressLoading = ref(false)
const tallerAddressNotice = ref('')
const addressLoading = ref(false)
const addressNotice = ref('')
const tallerPhotoInput = ref(null)
const tallerPhotoPreview = ref('')
const roleDraft = reactive({ name: '', description: '', permissions: 'ORDERS_READ, ORDERS_WRITE' })
const loginForm = reactive({ email: 'admin@taller.local', password: 'Admin123!Seguro' })
const registerForm = reactive({ fullName: '', email: '', password: '' })
const forgotForm = reactive({ email: '' })
const resetForm = reactive({ token: '', newPassword: '' })
const customerForm = reactive({
  fullName: '', alternateFullName: '', personalPhone: '', workPhone: '', age: '', birthDate: '', email: '',
  workEmail: '', photo: null, street: '', neighborhood: '', municipality: '', state: '', postalCode: '', rfc: '', curp: '', tallerId: ''
})
const tallerForm = reactive({
  nombreTaller: '', razonSocial: '', rfc: '', emailContacto: '', telefono: '', calle: '', municipio: '',
  estado: '', codigoPostal: '', colonia: '', foto: null
})
const vehicleForm = reactive({ vin: '', placa: '', marcaId: '', modeloId: '', nuevaMarca: '', nuevoModelo: '', anio: '', version: '', color: '', numeroSerie: '', kilometraje: '', tipoCombustible: '', numeroCilindros: '', tipoTransmision: '' })
const vehicleEditForm = reactive({ placa: '', marcaId: '', modeloId: '', nuevaMarca: '', nuevoModelo: '', anio: '', version: '', color: '', numeroSerie: '', kilometraje: '', tipoCombustible: '', numeroCilindros: '', tipoTransmision: '' })
const vehicleSearch = reactive({ type: 'vin', value: '' })

const isAdmin = computed(() => user.value?.roles?.some((role) => role.permissions?.includes('SECURITY_MANAGE_ROLES')))
const canRegisterCustomers = computed(() => user.value?.roles?.some((role) => role.permissions?.includes('CLIENTS_WRITE')))
const canReadCustomers = computed(() => user.value?.roles?.some((role) => role.permissions?.includes('CLIENTS_READ') || role.permissions?.includes('CLIENTS_WRITE')))
const rfcPattern = /^[A-ZÑ&]{3,4}\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[A-Z0-9]{3}$/
const curpPattern = /^[A-Z][AEIOUX][A-Z]{2}\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[HM](AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS)[B-DF-HJ-NP-TV-Z]{3}[A-Z\d]\d$/

async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) }
  if (!(options.body instanceof FormData)) headers['Content-Type'] = 'application/json'
  if (token.value) headers.Authorization = `Bearer ${token.value}`
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.message || 'No fue posible completar la operacion')
  return data
}

function setAuth(response) {
  token.value = response.token
  user.value = response.user
  localStorage.setItem('auth_token', response.token)
  if (isAdmin.value) loadRoles()
  if (isAdmin.value) loadStatuses()
  loadTalleres()
}

async function submitLogin() {
  await run(async () => {
    setAuth(await api('/auth/login', { method: 'POST', body: JSON.stringify(loginForm) }))
    success.value = 'Sesion iniciada correctamente'
  })
}

async function submitRegister() {
  await run(async () => {
    setAuth(await api('/auth/register', { method: 'POST', body: JSON.stringify(registerForm) }))
    success.value = 'Cuenta creada con rol CLIENTE'
  })
}

async function submitForgot() {
  await run(async () => {
    const response = await api('/auth/forgot-password', { method: 'POST', body: JSON.stringify(forgotForm) })
    resetToken.value = response.resetToken || ''
    resetForm.token = response.resetToken || ''
    success.value = response.message
  })
}

async function submitReset() {
  await run(async () => {
    const response = await api('/auth/reset-password', { method: 'POST', body: JSON.stringify(resetForm) })
    success.value = response.message
    mode.value = 'login'
  })
}

async function loadMe() {
  if (!token.value) return
  try {
    user.value = await api('/auth/me')
    if (isAdmin.value) await loadRoles()
    if (isAdmin.value) await loadStatuses()
    await loadTalleres()
  } catch {
    logout()
  }
}

async function loadRoles() { roles.value = await api('/roles') }
async function loadTalleres() { if (token.value) talleres.value = await api('/v1/talleres') }
async function loadStatuses() { if (isAdmin.value) statuses.value = await api('/v1/estatus') }
async function saveStatus() { await run(async () => { const path = statusDraft.id ? `/v1/estatus/${statusDraft.id}` : '/v1/estatus'; await api(path, { method: statusDraft.id ? 'PUT' : 'POST', body: JSON.stringify({ strValor: statusDraft.strValor, strDescripcion: statusDraft.strDescripcion }) }); Object.assign(statusDraft, { id: null, strValor: '', strDescripcion: '' }); await loadStatuses(); success.value = 'Estatus guardado correctamente' }) }
async function deactivateStatus(status) { await run(async () => { await api(`/v1/estatus/${status.id}`, { method: 'DELETE' }); await loadStatuses(); success.value = `Estatus desactivado: ${status.strValor}` }) }
function editStatus(status) { Object.assign(statusDraft, { id: status.id, strValor: status.strValor, strDescripcion: status.strDescripcion || '' }) }
async function openVehicleModal(customer) { vehicleModalCustomer.value = customer; Object.assign(vehicleForm, { vin: '', placa: '', marcaId: '', modeloId: '', nuevaMarca: '', nuevoModelo: '', anio: '', version: '', color: '', numeroSerie: '', kilometraje: '', tipoCombustible: '', numeroCilindros: '', tipoTransmision: '' }); vehicleBrands.value = await api('/v1/vehiculos/marcas') }
async function loadVehicleModels() { vehicleModels.value = vehicleForm.marcaId ? await api(`/v1/vehiculos/marcas/${vehicleForm.marcaId}/modelos`) : []; vehicleForm.modeloId = '' }
function applyVehicleModel() { const model = vehicleModels.value.find(item => String(item.id) === String(vehicleForm.modeloId)); if (!model) return; vehicleForm.tipoCombustible = model.tipoCombustible || ''; vehicleForm.numeroCilindros = model.numeroCilindros || ''; vehicleForm.tipoTransmision = model.tipoTransmision || '' }
function vehiclePayload(form) { return { ...form, marcaId: form.marcaId || null, modeloId: form.modeloId || null, anio: Number(form.anio), kilometraje: Number(form.kilometraje) } }
async function submitVehicle() { await run(async () => { if (!vehicleModalCustomer.value) return; const vehicle = await api('/v1/vehiculos', { method: 'POST', body: JSON.stringify({ ...vehiclePayload(vehicleForm), clienteId: vehicleModalCustomer.value.id }) }); vehicleModalCustomer.value = null; success.value = `Vehiculo registrado: ${vehicle.placa}` }) }
function openVehicleSearch() { activeView.value = 'vehiculos'; vehicleDetail.value = null; vehicleSearch.value = '' }
async function searchVehicle() { await run(async () => { if (!vehicleSearch.value.trim()) throw new Error('Captura un identificador de vehiculo'); const params = new URLSearchParams({ [vehicleSearch.type]: vehicleSearch.value.trim() }); vehicleDetail.value = await api(`/v1/vehiculos/ficha?${params}`) }) }
async function refreshVehicleDetail() { if (vehicleDetail.value?.id) vehicleDetail.value = await api(`/v1/vehiculos/ficha?id=${vehicleDetail.value.id}`) }
async function openVehicleEditModal(vehicle) {
  await run(async () => {
    vehicleBrands.value = await api('/v1/vehiculos/marcas')
    vehicleEditModels.value = vehicle.marcaId ? await api(`/v1/vehiculos/marcas/${vehicle.marcaId}/modelos`) : []
    Object.assign(vehicleEditForm, { placa: vehicle.placa, marcaId: String(vehicle.marcaId || ''), modeloId: String(vehicle.modeloId || ''), nuevaMarca: '', nuevoModelo: '', anio: String(vehicle.anio), version: vehicle.version, color: vehicle.color, numeroSerie: vehicle.numeroSerie, kilometraje: String(vehicle.kilometraje), tipoCombustible: vehicle.tipoCombustible, numeroCilindros: vehicle.numeroCilindros, tipoTransmision: vehicle.tipoTransmision })
    vehicleEditModal.value = vehicle
  })
}
async function loadVehicleEditModels() { vehicleEditModels.value = vehicleEditForm.marcaId ? await api(`/v1/vehiculos/marcas/${vehicleEditForm.marcaId}/modelos`) : []; vehicleEditForm.modeloId = '' }
function applyVehicleEditModel() { const model = vehicleEditModels.value.find(item => String(item.id) === String(vehicleEditForm.modeloId)); if (!model) return; vehicleEditForm.tipoCombustible = model.tipoCombustible || ''; vehicleEditForm.numeroCilindros = model.numeroCilindros || ''; vehicleEditForm.tipoTransmision = model.tipoTransmision || '' }
async function submitVehicleUpdate() { await run(async () => { if (!vehicleEditModal.value) return; const updated = await api(`/v1/vehiculos/${vehicleEditModal.value.id}`, { method: 'PUT', body: JSON.stringify(vehiclePayload(vehicleEditForm)) }); vehicleEditModal.value = null; await refreshVehicleDetail(); success.value = `Vehiculo actualizado: ${updated.placa}` }) }
async function openVehicleStatusModal(vehicle) { await run(async () => { await loadStatuses(); vehicleStatusModal.value = vehicle; selectedVehicleStatusId.value = String(statuses.value.find(status => status.strValor.toUpperCase() === vehicle.estatus.toUpperCase())?.id || ''); confirmVehicleStatusChange.value = false }) }
async function updateVehicleStatus() { await run(async () => { if (!vehicleStatusModal.value || !selectedVehicleStatusId.value) throw new Error('Selecciona un estatus'); if (!confirmVehicleStatusChange.value) throw new Error('Confirma el cambio de estado'); await api(`/v1/vehiculos/${vehicleStatusModal.value.id}/estatus`, { method: 'PUT', body: JSON.stringify({ statusId: Number(selectedVehicleStatusId.value) }) }); vehicleStatusModal.value = null; await refreshVehicleDetail(); success.value = 'Estado del vehiculo actualizado correctamente' }) }

async function loadCustomers(page = 0) {
  if (!canReadCustomers.value) return
  customersLoading.value = true
  try {
    const params = new URLSearchParams({ page: String(page), size: '10', sortBy: 'nombre', sortDir: customerSortDir.value })
    if (customerFilterTallerId.value) params.set('tallerId', customerFilterTallerId.value)
    customersPage.value = await api(`/v1/clientes?${params}`)
  } finally { customersLoading.value = false }
}

async function createRole() {
  await run(async () => {
    const permissions = roleDraft.permissions.split(',').map((item) => item.trim()).filter(Boolean)
    await api('/roles', { method: 'POST', body: JSON.stringify({ name: roleDraft.name, description: roleDraft.description, permissions }) })
    roleDraft.name = ''
    roleDraft.description = ''
    roleDraft.permissions = 'ORDERS_READ'
    await loadRoles()
    success.value = 'Rol creado correctamente'
  })
}

function uniqueByName(items, key) {
  return [...new Map(items.filter(Boolean).map((item) => [item[key], item])).values()]
}

async function loadStates() {
  try {
    sepomexStates.value = await findStates()
  } catch {
    addressNotice.value = 'El catalogo de direcciones no esta disponible. Puedes capturar la direccion manualmente.'
  }
}

async function applyZipCode() {
  const zipCode = customerForm.postalCode
  if (!/^\d{5}$/.test(zipCode)) return

  addressLoading.value = true
  addressNotice.value = ''
  try {
    const zipCodes = await findZipCode(zipCode)
    if (!zipCodes.length) {
      sepomexNeighborhoods.value = []
      addressNotice.value = 'No se encontraron colonias para este codigo postal.'
      return
    }

    const first = zipCodes[0]
    customerForm.state = first.d_estado || ''
    customerForm.municipality = first.d_mnpio || ''
    sepomexNeighborhoods.value = uniqueByName(zipCodes, 'd_asenta')
    customerForm.neighborhood = sepomexNeighborhoods.value.some((item) => item.d_asenta === customerForm.neighborhood)
      ? customerForm.neighborhood
      : ''
    const matchingState = sepomexStates.value.find((state) => state.name === customerForm.state)
    selectedStateId.value = matchingState?.id ? String(matchingState.id) : ''
    if (selectedStateId.value) await loadMunicipalities()
    addressNotice.value = 'Estado, municipio y colonias actualizados desde SEPOMEX.'
  } catch {
    addressNotice.value = 'No fue posible consultar SEPOMEX. Puedes completar la direccion manualmente.'
  } finally {
    addressLoading.value = false
  }
}

async function loadMunicipalities() {
  if (!selectedStateId.value) return
  addressLoading.value = true
  try {
    sepomexMunicipalities.value = await findMunicipalities(selectedStateId.value)
    const selected = sepomexMunicipalities.value.find((item) => item.name === customerForm.municipality)
    selectedMunicipalityId.value = selected?.id ? String(selected.id) : ''
  } catch {
    addressNotice.value = 'No fue posible obtener los municipios del estado seleccionado.'
  } finally {
    addressLoading.value = false
  }
}

async function selectState() {
  const state = sepomexStates.value.find((item) => String(item.id) === selectedStateId.value)
  customerForm.state = state?.name || ''
  customerForm.municipality = ''
  customerForm.neighborhood = ''
  selectedMunicipalityId.value = ''
  sepomexNeighborhoods.value = []
  await loadMunicipalities()
}

async function selectMunicipality() {
  const municipality = sepomexMunicipalities.value.find((item) => String(item.id) === selectedMunicipalityId.value)
  customerForm.municipality = municipality?.name || ''
  customerForm.neighborhood = ''
  sepomexNeighborhoods.value = []
  if (!customerForm.state || !customerForm.municipality) return

  addressLoading.value = true
  try {
    sepomexNeighborhoods.value = uniqueByName(
      await findNeighborhoods(customerForm.state, customerForm.municipality),
      'd_asenta'
    )
  } catch {
    addressNotice.value = 'No fue posible obtener las colonias del municipio seleccionado.'
  } finally {
    addressLoading.value = false
  }
}

async function applyTallerZipCode() {
  const zipCode = tallerForm.codigoPostal
  if (!/^\d{5}$/.test(zipCode)) return
  tallerAddressLoading.value = true
  tallerAddressNotice.value = ''
  try {
    const zipCodes = await findZipCode(zipCode)
    if (!zipCodes.length) {
      tallerNeighborhoods.value = []
      tallerAddressNotice.value = 'No se encontraron colonias para este codigo postal.'
      return
    }
    const first = zipCodes[0]
    tallerForm.estado = first.d_estado || ''
    tallerForm.municipio = first.d_mnpio || ''
    tallerNeighborhoods.value = uniqueByName(zipCodes, 'd_asenta')
    tallerForm.colonia = tallerNeighborhoods.value.some((item) => item.d_asenta === tallerForm.colonia)
      ? tallerForm.colonia : ''
    const matchingState = sepomexStates.value.find((state) => state.name === tallerForm.estado)
    tallerSelectedStateId.value = matchingState?.id ? String(matchingState.id) : ''
    if (tallerSelectedStateId.value) await loadTallerMunicipalities()
    tallerAddressNotice.value = 'Estado, municipio y colonias actualizados desde SEPOMEX.'
  } catch {
    tallerAddressNotice.value = 'No fue posible consultar SEPOMEX. Puedes completar la direccion manualmente.'
  } finally { tallerAddressLoading.value = false }
}

async function loadTallerMunicipalities() {
  if (!tallerSelectedStateId.value) return
  tallerAddressLoading.value = true
  try {
    tallerMunicipalities.value = await findMunicipalities(tallerSelectedStateId.value)
    const selected = tallerMunicipalities.value.find((item) => item.name === tallerForm.municipio)
    tallerSelectedMunicipalityId.value = selected?.id ? String(selected.id) : ''
  } catch {
    tallerAddressNotice.value = 'No fue posible obtener los municipios del estado seleccionado.'
  } finally { tallerAddressLoading.value = false }
}

async function selectTallerState() {
  const state = sepomexStates.value.find((item) => String(item.id) === tallerSelectedStateId.value)
  tallerForm.estado = state?.name || ''
  tallerForm.municipio = ''
  tallerForm.colonia = ''
  tallerSelectedMunicipalityId.value = ''
  tallerNeighborhoods.value = []
  await loadTallerMunicipalities()
}

async function selectTallerMunicipality() {
  const municipality = tallerMunicipalities.value.find((item) => String(item.id) === tallerSelectedMunicipalityId.value)
  tallerForm.municipio = municipality?.name || ''
  tallerForm.colonia = ''
  tallerNeighborhoods.value = []
  if (!tallerForm.estado || !tallerForm.municipio) return
  tallerAddressLoading.value = true
  try {
    tallerNeighborhoods.value = uniqueByName(await findNeighborhoods(tallerForm.estado, tallerForm.municipio), 'd_asenta')
  } catch {
    tallerAddressNotice.value = 'No fue posible obtener las colonias del municipio seleccionado.'
  } finally { tallerAddressLoading.value = false }
}

function clearCustomerPhoto(resetInput = true) {
  if (customerPhotoPreview.value) URL.revokeObjectURL(customerPhotoPreview.value)
  customerPhotoPreview.value = ''
  customerForm.photo = null
  if (resetInput && customerPhotoInput.value) customerPhotoInput.value.value = ''
}

function selectCustomerPhoto(event) {
  const photo = event.dataTransfer?.files?.[0] || event.target.files?.[0] || null
  clearCustomerPhoto(false)
  if (!photo) return
  if (photo.size > 15 * 1024 * 1024) { error.value = 'La fotografia no puede superar 15 MB'; return }
  customerForm.photo = photo
  customerPhotoPreview.value = URL.createObjectURL(photo)
}

async function submitCustomer() {
  await run(async () => {
    if (!editingCustomerId.value && !customerForm.photo) throw new Error('Selecciona una fotografia para el cliente')
    if (customerForm.rfc && !rfcPattern.test(customerForm.rfc.toUpperCase())) throw new Error('El RFC no tiene un formato valido')
    if (!curpPattern.test(customerForm.curp.toUpperCase())) throw new Error('La CURP no tiene un formato valido')
    if (!customerForm.tallerId) throw new Error('Selecciona el taller del cliente')
    if (editingCustomerId.value) {
      const payload = Object.fromEntries(Object.entries(customerForm).filter(([key]) => key !== 'photo'))
      payload.rfc = payload.rfc || null
      payload.workEmail = payload.workEmail || null
      const customer = await api(`/v1/clientes/${editingCustomerId.value}`, { method: 'PUT', body: JSON.stringify(payload) })
      editingCustomerId.value = null
      success.value = `Cliente actualizado: ${customer.fullName}`
      await loadCustomers(customersPage.value.number)
      resetCustomerForm()
      return
    }
    const formData = new FormData()
    Object.entries(customerForm).forEach(([key, value]) => { if (value !== null && value !== '') formData.append(key, value) })
    const customer = await api('/customers', { method: 'POST', body: formData })
    resetCustomerForm()
    await loadCustomers(0)
    success.value = `Nuevo cliente creado: ${customer.fullName}`
  })
}

function resetCustomerForm() {
  Object.assign(customerForm, { fullName: '', alternateFullName: '', personalPhone: '', workPhone: '', age: '', birthDate: '', email: '', workEmail: '', photo: null, street: '', neighborhood: '', municipality: '', state: '', postalCode: '', rfc: '', curp: '', tallerId: '' })
  clearCustomerPhoto()
}

function editCustomer(customer) {
  Object.assign(customerForm, { fullName: customer.fullName, alternateFullName: customer.alternateFullName, personalPhone: customer.personalPhone, workPhone: customer.workPhone, age: customer.age, birthDate: customer.birthDate, email: customer.email, workEmail: customer.workEmail || '', photo: null, street: customer.street, neighborhood: customer.neighborhood, municipality: customer.municipality, state: customer.state, postalCode: customer.postalCode, rfc: customer.rfc || '', curp: customer.curp, tallerId: String(customer.tallerId) })
  editingCustomerId.value = customer.id
  activeView.value = 'registrar-cliente'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function openCustomerRegistration() {
  editingCustomerId.value = null
  resetCustomerForm()
  activeView.value = 'registrar-cliente'
}

async function suspendCustomer(customer) {
  await run(async () => {
    await api(`/v1/clientes/${customer.id}/suspender`, { method: 'PUT' })
    success.value = `Cliente suspendido: ${customer.fullName}`
    await loadCustomers(customersPage.value.number)
  })
}

async function activateCustomer(customer) {
  await run(async () => {
    await api(`/v1/clientes/${customer.id}/activar`, { method: 'PUT' })
    success.value = `Cliente reactivado: ${customer.fullName}`
    await loadCustomers(customersPage.value.number)
  })
}

async function openCustomerStatusModal(customer) {
  await loadStatuses()
  customerStatusModal.value = customer
  selectedCustomerStatusId.value = String(statuses.value.find(status => status.strValor.toUpperCase() === customer.status.toUpperCase())?.id || '')
}

async function updateCustomerStatus() {
  await run(async () => {
    if (!customerStatusModal.value || !selectedCustomerStatusId.value) throw new Error('Selecciona un estatus')
    await api(`/v1/clientes/${customerStatusModal.value.id}/estatus?statusId=${selectedCustomerStatusId.value}`, { method: 'PUT' })
    success.value = `Estatus actualizado: ${customerStatusModal.value.fullName}`
    customerStatusModal.value = null
    await loadCustomers(customersPage.value.number)
  })
}

function clearTallerPhoto() {
  if (tallerPhotoPreview.value) URL.revokeObjectURL(tallerPhotoPreview.value)
  tallerPhotoPreview.value = ''
  tallerForm.foto = null
  if (tallerPhotoInput.value) tallerPhotoInput.value.value = ''
}

function selectTallerPhoto(event) {
  const photo = event.dataTransfer?.files?.[0] || event.target.files?.[0] || null
  clearTallerPhoto()
  if (!photo) return
  if (photo.size > 15 * 1024 * 1024) { error.value = 'La fotografia no puede superar 15 MB'; return }
  tallerForm.foto = photo
  tallerPhotoPreview.value = URL.createObjectURL(photo)
}

async function submitTaller() {
  await run(async () => {
    if (!rfcPattern.test(tallerForm.rfc.toUpperCase())) throw new Error('El RFC no tiene un formato valido')
    if (!tallerForm.foto) throw new Error('Selecciona una fotografia para el taller')
    const formData = new FormData()
    Object.entries(tallerForm).forEach(([key, value]) => { if (value) formData.append(key, value) })
    const taller = await api('/v1/talleres', { method: 'POST', body: formData })
    Object.assign(tallerForm, { nombreTaller: '', razonSocial: '', rfc: '', emailContacto: '', telefono: '', calle: '', municipio: '', estado: '', codigoPostal: '', colonia: '', foto: null })
    clearTallerPhoto()
    await loadTalleres()
    success.value = `Taller creado: ${taller.nombreTaller}`
  })
}

async function run(action) {
  loading.value = true
  error.value = ''
  success.value = ''
  try { await action() } catch (err) { error.value = err.message } finally { loading.value = false }
}

function logout() {
  clearCustomerPhoto()
  token.value = ''
  user.value = null
  roles.value = []
  talleres.value = []
  customersPage.value = { content: [], number: 0, totalPages: 0, totalElements: 0 }
  localStorage.removeItem('auth_token')
  mode.value = 'login'
}

watch(() => customerForm.postalCode, applyZipCode)
watch(() => tallerForm.codigoPostal, applyTallerZipCode)
watch(customerFilterTallerId, () => loadCustomers(0))
watch(customerSortDir, () => loadCustomers(0))
onMounted(async () => {
  await Promise.all([loadMe(), loadStates()])
})
</script>

<template>
  <main class="min-h-screen bg-[#FAF8F5] font-sans text-slate-800">
    <header class="border-b border-rose-100 bg-white/95 shadow-sm">
      <div class="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-5 sm:px-6 lg:px-8">
        <div>
          <p class="text-xs font-bold uppercase tracking-[0.18em] text-rose-400">Taller mecanico</p>
          <h1 class="mt-1 text-2xl font-bold tracking-tight text-slate-900">Refaccionaria</h1>
        </div>
        <button v-if="user" class="rounded-lg border border-rose-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-rose-400 hover:bg-rose-50 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="logout">
          Cerrar sesion
        </button>
      </div>
    </header>

    <section class="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-10" :class="user ? '' : 'max-w-lg'">
      <div class="grid gap-6" :class="user ? 'lg:grid-cols-[300px_minmax(0,1fr)]' : ''">
        <aside class="rounded-xl border border-slate-200 bg-white p-5 shadow-md shadow-rose-950/5">
          <div v-if="!user" class="space-y-5">
            <div class="grid grid-cols-2 rounded-lg border border-rose-100 bg-rose-50 p-1">
              <button class="rounded-md px-3 py-2 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="mode === 'login' ? 'bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 text-white' : 'text-slate-600 hover:text-rose-700'" @click="mode = 'login'">Login</button>
              <button class="rounded-md px-3 py-2 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="mode === 'register' ? 'bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 text-white' : 'text-slate-600 hover:text-rose-700'" @click="mode = 'register'">Registro</button>
            </div>

            <form v-if="mode === 'login'" class="space-y-4" @submit.prevent="submitLogin">
              <h2 class="text-xl font-bold text-slate-900">Iniciar sesion</h2>
              <label class="block text-sm font-semibold text-slate-700">Correo<input v-model="loginForm.email" type="email" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 placeholder-slate-400 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <label class="block text-sm font-semibold text-slate-700">Contrasena<input v-model="loginForm.password" type="password" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <button class="w-full rounded-lg bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 px-4 py-2.5 font-bold text-white transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-rose-400 disabled:opacity-60" :disabled="loading">Entrar</button>
              <button type="button" class="text-sm font-semibold text-rose-700 hover:text-rose-800 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="mode = 'forgot'">Recuperar contrasena</button>
            </form>

            <form v-if="mode === 'register'" class="space-y-4" @submit.prevent="submitRegister">
              <h2 class="text-xl font-bold text-slate-900">Crear usuario cliente</h2>
              <label class="block text-sm font-semibold text-slate-700">Nombre completo<input v-model="registerForm.fullName" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <label class="block text-sm font-semibold text-slate-700">Correo<input v-model="registerForm.email" type="email" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <label class="block text-sm font-semibold text-slate-700">Contrasena segura<input v-model="registerForm.password" type="password" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <button class="w-full rounded-lg bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 px-4 py-2.5 font-bold text-white transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-rose-400 disabled:opacity-60" :disabled="loading">Registrar</button>
            </form>

            <form v-if="mode === 'forgot'" class="space-y-4" @submit.prevent="submitForgot">
              <h2 class="text-xl font-bold text-slate-900">Recuperar contrasena</h2>
              <label class="block text-sm font-semibold text-slate-700">Correo registrado<input v-model="forgotForm.email" type="email" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <button class="w-full rounded-lg bg-amber-400 px-4 py-2.5 font-bold text-slate-950 transition hover:bg-amber-300 focus:outline-none focus:ring-2 focus:ring-rose-400" :disabled="loading">Generar token</button>
              <button type="button" class="text-sm font-semibold text-rose-700 hover:text-rose-800 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="mode = 'reset'">Ya tengo token</button>
            </form>

            <form v-if="mode === 'reset'" class="space-y-4" @submit.prevent="submitReset">
              <h2 class="text-xl font-bold text-slate-900">Restablecer contrasena</h2>
              <label class="block text-sm font-semibold text-slate-700">Token<input v-model="resetForm.token" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <label class="block text-sm font-semibold text-slate-700">Nueva contrasena<input v-model="resetForm.newPassword" type="password" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <button class="w-full rounded-lg bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 px-4 py-2.5 font-bold text-white transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-rose-400" :disabled="loading">Actualizar</button>
            </form>
          </div>

          <div v-else class="space-y-4">
            <p class="text-xs font-bold uppercase tracking-[0.16em] text-slate-500">Sesion activa</p>
            <div class="rounded-lg border border-slate-200 bg-white p-4">
              <p class="font-bold text-slate-900">{{ user.fullName }}</p><p class="mt-1 text-sm text-slate-500">{{ user.email }}</p>
            </div>
            <div class="flex flex-wrap gap-2"><span v-for="role in user.roles" :key="role.id" class="rounded-md border border-rose-200 bg-rose-50 px-2.5 py-1 text-xs font-bold text-rose-700">{{ role.name }}</span></div>
            <nav class="border-t border-slate-100 pt-4" aria-label="Navegacion principal">
              <p class="mb-2 text-xs font-bold uppercase tracking-[0.16em] text-slate-500">Navegacion</p>
              <div class="space-y-1">
                <button type="button" class="w-full rounded-lg px-3 py-2 text-left text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="activeView === 'inicio' ? 'bg-rose-50 text-rose-800' : 'text-slate-600 hover:bg-slate-50'" @click="activeView = 'inicio'">Inicio</button>
                <button v-if="isAdmin" type="button" class="w-full rounded-lg px-3 py-2 text-left text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="activeView === 'talleres' ? 'bg-rose-50 text-rose-800' : 'text-slate-600 hover:bg-slate-50'" @click="activeView = 'talleres'">Administracion: Talleres</button>
                <button v-if="isAdmin" type="button" class="w-full rounded-lg px-3 py-2 text-left text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="activeView === 'estatus' ? 'bg-rose-50 text-rose-800' : 'text-slate-600 hover:bg-slate-50'" @click="activeView = 'estatus'; loadStatuses()">Administracion: Estatus</button>
                <button v-if="canRegisterCustomers" type="button" class="w-full rounded-lg px-3 py-2 text-left text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="activeView === 'registrar-cliente' ? 'bg-rose-50 text-rose-800' : 'text-slate-600 hover:bg-slate-50'" @click="openCustomerRegistration">Clientes: Registrar cliente</button>
                <button v-if="canReadCustomers" type="button" class="w-full rounded-lg px-3 py-2 text-left text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="activeView === 'administrar-clientes' ? 'bg-rose-50 text-rose-800' : 'text-slate-600 hover:bg-slate-50'" @click="activeView = 'administrar-clientes'; loadCustomers()">Clientes: Administrar (UC-CV-02)</button>
                <button v-if="canReadCustomers" type="button" class="w-full rounded-lg px-3 py-2 text-left text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="activeView === 'vehiculos' ? 'bg-rose-50 text-rose-800' : 'text-slate-600 hover:bg-slate-50'" @click="openVehicleSearch">Vehiculos: Buscar y administrar</button>
              </div>
            </nav>
          </div>

          <div v-if="error" class="mt-4 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-semibold text-rose-700" role="alert">{{ error }}</div>
          <div v-if="success" class="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm font-semibold text-emerald-700" role="status">{{ success }}</div>
          <div v-if="resetToken" class="mt-3 break-all rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs font-semibold text-amber-800">Token dev: {{ resetToken }}</div>
        </aside>

        <div v-if="user" class="space-y-6">
          <section v-if="activeView === 'inicio'" class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <p class="text-sm text-slate-500">Inicio</p><h2 class="mt-1 text-2xl font-bold text-slate-900">Operacion del taller</h2>
            <p class="mt-2 max-w-2xl text-sm leading-6 text-slate-500">Gestiona clientes con una vista concentrada, responsive y conectada a los servicios autorizados de la plataforma.</p>
          </section>

          <section v-if="activeView === 'talleres' && isAdmin" class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <div><p class="text-xs font-bold uppercase tracking-[0.16em] text-rose-400">Administracion</p><h2 class="mt-1 text-2xl font-bold text-slate-900">Registrar taller</h2><p class="mt-2 text-sm text-slate-500">Crea talleres activos para asociar clientes de la misma empresa.</p></div>
            <form class="mt-6 space-y-5" @submit.prevent="submitTaller">
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <label class="text-sm font-semibold text-slate-700">Nombre del taller<input v-model.trim="tallerForm.nombreTaller" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                <label class="text-sm font-semibold text-slate-700">Razon social<input v-model.trim="tallerForm.razonSocial" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                <label class="text-sm font-semibold text-slate-700">RFC<input v-model.trim="tallerForm.rfc" maxlength="13" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 uppercase focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                <label class="text-sm font-semibold text-slate-700">Email de contacto<input v-model.trim="tallerForm.emailContacto" type="email" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                <label class="text-sm font-semibold text-slate-700">Telefono<input v-model.trim="tallerForm.telefono" type="tel" placeholder="+525512345678" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                <label class="text-sm font-semibold text-slate-700">Codigo postal<input v-model.trim="tallerForm.codigoPostal" inputmode="numeric" pattern="[0-9]{5}" maxlength="5" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                <label v-if="sepomexStates.length" class="text-sm font-semibold text-slate-700">Estado<select v-model="tallerSelectedStateId" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required @change="selectTallerState"><option disabled value="">Selecciona un estado</option><option v-for="state in sepomexStates" :key="state.id" :value="String(state.id)">{{ state.name }}</option></select></label>
                <label v-else class="text-sm font-semibold text-slate-700">Estado<input v-model.trim="tallerForm.estado" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                <label v-if="tallerMunicipalities.length" class="text-sm font-semibold text-slate-700">Municipio<select v-model="tallerSelectedMunicipalityId" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required @change="selectTallerMunicipality"><option disabled value="">Selecciona un municipio</option><option v-for="municipality in tallerMunicipalities" :key="municipality.id" :value="String(municipality.id)">{{ municipality.name }}</option></select></label>
                <label v-else class="text-sm font-semibold text-slate-700">Municipio<input v-model.trim="tallerForm.municipio" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                <label v-if="tallerNeighborhoods.length" class="text-sm font-semibold text-slate-700">Colonia<select v-model="tallerForm.colonia" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required><option disabled value="">Selecciona una colonia</option><option v-for="neighborhood in tallerNeighborhoods" :key="neighborhood.id" :value="neighborhood.d_asenta">{{ neighborhood.d_asenta }}</option></select></label>
                <label v-else class="text-sm font-semibold text-slate-700">Colonia<input v-model.trim="tallerForm.colonia" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                <label class="text-sm font-semibold text-slate-700">Calle<input v-model.trim="tallerForm.calle" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              </div>
              <p v-if="tallerAddressLoading" class="text-sm font-semibold text-amber-700">Consultando catalogo de direcciones...</p>
              <p v-else-if="tallerAddressNotice" class="text-sm text-rose-700" role="status">{{ tallerAddressNotice }}</p>
              <div><input ref="tallerPhotoInput" type="file" accept="image/jpeg,image/png,image/webp" class="sr-only" @change="selectTallerPhoto" /><div class="flex min-h-36 flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-300 bg-[#FFFCFA] p-4 text-center"><img v-if="tallerPhotoPreview" :src="tallerPhotoPreview" alt="Previsualizacion del taller" class="h-20 w-20 rounded-lg object-cover" /><p class="mt-2 text-sm font-semibold text-slate-700">{{ tallerPhotoPreview ? 'Fotografia seleccionada' : 'Fotografia del taller' }}</p><p class="text-xs text-slate-500">JPEG, PNG o WEBP. Maximo 15 MB.</p><button type="button" class="mt-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:border-rose-400" @click="tallerPhotoInput?.click()">Seleccionar archivo</button></div></div>
              <button class="w-full rounded-lg bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 px-4 py-3 font-bold text-white transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-rose-400 disabled:opacity-60" :disabled="loading">Crear taller</button>
            </form>
          </section>

          <section v-if="activeView === 'registrar-cliente' && canRegisterCustomers" class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <div class="flex flex-wrap items-start justify-between gap-3"><div><p class="text-xs font-bold uppercase tracking-[0.16em] text-rose-400">Clientes</p><h2 class="mt-1 text-2xl font-bold text-slate-900">{{ editingCustomerId ? 'Editar cliente' : 'Registrar nuevo cliente' }}</h2></div><span class="rounded-full border border-rose-200 bg-rose-50 px-3 py-1 text-xs font-bold text-rose-700">Acceso autorizado</span></div>

            <form class="mt-6 space-y-6" @submit.prevent="submitCustomer">
              <fieldset class="rounded-xl border border-slate-200 bg-[#FFFCFA] p-4 shadow-sm sm:p-5"><legend class="px-2 text-sm font-bold text-slate-800">Informacion personal</legend>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <label class="text-sm font-semibold text-slate-700">Nombre completo<input v-model.trim="customerForm.fullName" pattern="[A-Za-zÁÉÍÓÚáéíóúÑñ ]+" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-700">Nombre alternativo<input v-model.trim="customerForm.alternateFullName" pattern="[A-Za-zÁÉÍÓÚáéíóúÑñ ]+" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-700">Edad<input v-model.number="customerForm.age" type="number" min="0" max="130" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-700">Fecha de nacimiento<input v-model="customerForm.birthDate" type="date" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-700">RFC <span class="font-normal text-slate-500">(opcional)</span><input v-model.trim="customerForm.rfc" maxlength="13" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 uppercase text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" /></label>
                  <label class="text-sm font-semibold text-slate-700">CURP<input v-model.trim="customerForm.curp" maxlength="18" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 uppercase text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                </div>
              </fieldset>

              <fieldset class="rounded-xl border border-slate-200 bg-[#FFFCFA] p-4 shadow-sm sm:p-5"><legend class="px-2 text-sm font-bold text-slate-800">Contacto y fotografia</legend>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <label class="text-sm font-semibold text-slate-700">Telefono personal<input v-model.trim="customerForm.personalPhone" type="tel" placeholder="+525512345678" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 placeholder-slate-400 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-700">Telefono trabajo<input v-model.trim="customerForm.workPhone" type="tel" placeholder="+525512345678" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 placeholder-slate-400 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-700">Email principal<input v-model.trim="customerForm.email" type="email" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-700">Email trabajo<input v-model.trim="customerForm.workEmail" type="email" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" /></label>
                  <label class="text-sm font-semibold text-slate-700">Taller<select v-model="customerForm.tallerId" :disabled="editingCustomerId && !isAdmin" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 disabled:bg-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required><option disabled value="">Selecciona un taller activo</option><option v-for="taller in talleres" :key="taller.id" :value="String(taller.id)">{{ taller.nombreTaller }}</option></select></label>
                  <div class="sm:col-span-2">
                    <input ref="customerPhotoInput" type="file" accept="image/jpeg,image/png,image/webp" class="sr-only" :required="!editingCustomerId" @change="selectCustomerPhoto" />
                    <div class="flex min-h-44 flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-300 bg-white p-4 text-center transition hover:border-rose-400" @dragover.prevent @drop.prevent="selectCustomerPhoto">
                      <img v-if="customerPhotoPreview" :src="customerPhotoPreview" alt="Previsualizacion de fotografia" class="h-28 w-28 rounded-lg object-cover" />
                      <p class="mt-3 text-sm font-bold text-slate-800">{{ customerPhotoPreview ? 'Fotografia seleccionada' : 'Arrastra una fotografia aqui' }}</p>
                      <p class="mt-1 text-xs text-slate-500">JPEG, PNG o WEBP. Maximo 15 MB.</p>
                      <div class="mt-3 flex gap-2"><button type="button" class="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-800 hover:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="customerPhotoInput?.click()">Seleccionar archivo</button><button v-if="customerPhotoPreview" type="button" class="rounded-lg px-3 py-2 text-sm font-semibold text-slate-500 hover:text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="clearCustomerPhoto">Quitar</button></div>
                    </div>
                  </div>
                </div>
              </fieldset>

              <fieldset class="rounded-xl border border-rose-500/20 bg-[#FFFCFA] p-4 shadow-sm sm:p-5"><legend class="px-2 text-sm font-bold text-slate-800">Direccion completa</legend>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  <label class="text-sm font-semibold text-slate-700">Codigo postal<input v-model.trim="customerForm.postalCode" inputmode="numeric" pattern="[0-9]{5}" maxlength="5" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label v-if="sepomexStates.length" class="text-sm font-semibold text-slate-700">Estado<select v-model="selectedStateId" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required @change="selectState"><option disabled value="">Selecciona un estado</option><option v-for="state in sepomexStates" :key="state.id" :value="String(state.id)">{{ state.name }}</option></select></label>
                  <label v-else class="text-sm font-semibold text-slate-700">Estado<input v-model.trim="customerForm.state" pattern="[A-Za-zÁÉÍÓÚáéíóúÑñ ]+" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label v-if="sepomexMunicipalities.length" class="text-sm font-semibold text-slate-700">Municipio o alcaldia<select v-model="selectedMunicipalityId" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required @change="selectMunicipality"><option disabled value="">Selecciona un municipio</option><option v-for="municipality in sepomexMunicipalities" :key="municipality.id" :value="String(municipality.id)">{{ municipality.name }}</option></select></label>
                  <label v-else class="text-sm font-semibold text-slate-700">Municipio o alcaldia<input v-model.trim="customerForm.municipality" pattern="[A-Za-zÁÉÍÓÚáéíóúÑñ ]+" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label v-if="sepomexNeighborhoods.length" class="text-sm font-semibold text-slate-700">Colonia<select v-model="customerForm.neighborhood" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required><option disabled value="">Selecciona una colonia</option><option v-for="neighborhood in sepomexNeighborhoods" :key="neighborhood.id" :value="neighborhood.d_asenta">{{ neighborhood.d_asenta }}</option></select></label>
                  <label v-else class="text-sm font-semibold text-slate-700">Colonia<input v-model.trim="customerForm.neighborhood" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-700 lg:col-span-2">Calle<input v-model.trim="customerForm.street" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-slate-900 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                </div>
                <p v-if="addressLoading" class="mt-3 text-sm font-semibold text-amber-700">Consultando catalogo de direcciones...</p>
                <p v-else-if="addressNotice" class="mt-3 text-sm text-rose-700" role="status">{{ addressNotice }}</p>
              </fieldset>
              <div class="flex flex-wrap gap-3"><button class="flex-1 rounded-lg bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 px-4 py-3 font-bold text-white transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-rose-400 disabled:opacity-60" :disabled="loading">{{ editingCustomerId ? 'Guardar cambios' : 'Registrar cliente' }}</button><button v-if="editingCustomerId" type="button" class="rounded-lg border border-slate-300 px-4 py-3 font-semibold text-slate-700 hover:border-rose-400" @click="editingCustomerId = null; resetCustomerForm()">Cancelar edicion</button></div>
            </form>
          </section>

          <section v-if="activeView === 'administrar-clientes' && canReadCustomers" class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <div class="flex flex-wrap items-end justify-between gap-4"><div><p class="text-xs font-bold uppercase tracking-[0.16em] text-rose-400">UC-CV-02</p><h2 class="mt-1 text-2xl font-bold text-slate-900">Administracion de clientes</h2></div><div class="flex flex-wrap gap-2"><label class="text-sm font-semibold text-slate-700">Taller<select v-model="customerFilterTallerId" class="ml-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"><option value="">Todos</option><option v-for="taller in talleres" :key="taller.id" :value="String(taller.id)">{{ taller.nombreTaller }}</option></select></label><button type="button" class="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-semibold text-rose-700 hover:bg-rose-100" @click="customerSortDir = customerSortDir === 'asc' ? 'desc' : 'asc'">Nombre: {{ customerSortDir === 'asc' ? 'A-Z' : 'Z-A' }}</button></div></div>
            <div class="mt-5 overflow-x-auto">
              <table class="min-w-full text-left text-sm"><thead class="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500"><tr><th class="px-3 py-3">Cliente</th><th class="px-3 py-3">Contacto</th><th class="px-3 py-3">Taller</th><th class="px-3 py-3">Estado</th><th class="px-3 py-3">Acciones</th></tr></thead>
                <tbody><tr v-for="customer in customersPage.content" :key="customer.id" class="border-b border-slate-100"><td class="px-3 py-3 font-semibold text-slate-800">{{ customer.fullName }}</td><td class="px-3 py-3 text-slate-600">{{ customer.email }}<br>{{ customer.personalPhone }}</td><td class="px-3 py-3 text-slate-600">{{ customer.tallerNombre }}</td><td class="px-3 py-3"><span class="rounded-full px-2 py-1 text-xs font-bold" :class="customer.status.toUpperCase() === 'ACTIVO' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'">{{ customer.status }}</span></td><td class="px-3 py-3"><div class="flex flex-wrap gap-2"><button v-if="canRegisterCustomers" type="button" class="text-sm font-semibold text-rose-700 hover:text-rose-900" @click="editCustomer(customer)">Editar</button><button v-if="customer.status.toUpperCase() === 'ACTIVO'" type="button" class="text-sm font-semibold text-amber-700 hover:text-amber-900" @click="openVehicleModal(customer)">Añadir vehículo</button><button v-if="isAdmin" type="button" class="text-sm font-semibold text-slate-700 hover:text-slate-950" @click="openCustomerStatusModal(customer)">Estado</button></div></td></tr><tr v-if="!customersLoading && !customersPage.content.length"><td colspan="5" class="px-3 py-6 text-center text-slate-500">No hay clientes para el filtro seleccionado.</td></tr></tbody>
              </table>
            </div>
            <div class="mt-5 flex items-center justify-between gap-3 text-sm text-slate-600"><span>{{ customersLoading ? 'Cargando clientes...' : `Pagina ${customersPage.totalPages ? customersPage.number + 1 : 0} de ${customersPage.totalPages}` }}</span><div class="flex gap-2"><button type="button" class="rounded-lg border border-slate-300 px-3 py-2 font-semibold disabled:opacity-40" :disabled="customersPage.number === 0 || customersLoading" @click="loadCustomers(customersPage.number - 1)">Anterior</button><button type="button" class="rounded-lg border border-slate-300 px-3 py-2 font-semibold disabled:opacity-40" :disabled="customersPage.number + 1 >= customersPage.totalPages || customersLoading" @click="loadCustomers(customersPage.number + 1)">Siguiente</button></div></div>
          </section>

          <section v-if="activeView === 'vehiculos' && canReadCustomers" class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <div>
              <p class="text-xs font-bold uppercase tracking-[0.16em] text-rose-400">UC-CV-07, UC-CV-08 y UC-CV-09</p>
              <h2 class="mt-1 text-2xl font-bold text-slate-900">Busqueda de vehiculo</h2>
              <p class="mt-2 text-sm text-slate-500">Consulta la ficha por VIN, placa o identificador para editar datos o gestionar su estado.</p>
            </div>
            <form class="mt-5 grid gap-3 sm:grid-cols-[11rem_1fr_auto]" @submit.prevent="searchVehicle">
              <label class="text-sm font-semibold text-slate-700">Buscar por
                <select v-model="vehicleSearch.type" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400">
                  <option value="vin">VIN</option><option value="placa">Placa</option><option value="id">ID</option>
                </select>
              </label>
              <label class="text-sm font-semibold text-slate-700">Identificador
                <input v-model.trim="vehicleSearch.value" :inputmode="vehicleSearch.type === 'id' ? 'numeric' : 'text'" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 uppercase focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required />
              </label>
              <button class="self-end rounded-lg bg-rose-600 px-4 py-2.5 font-bold text-white hover:bg-rose-700 focus:outline-none focus:ring-2 focus:ring-rose-400">Buscar</button>
            </form>

            <article v-if="vehicleDetail" class="mt-6 border-t border-slate-100 pt-6">
              <div class="flex flex-wrap items-start justify-between gap-4">
                <div><p class="text-sm font-bold text-slate-900">{{ vehicleDetail.marca }} {{ vehicleDetail.modelo }} · {{ vehicleDetail.anio }}</p><p class="mt-1 text-sm text-slate-500">VIN: {{ vehicleDetail.vin }} · Placa: {{ vehicleDetail.placa }}</p></div>
                <span class="rounded-full px-3 py-1 text-xs font-bold" :class="vehicleDetail.estatus.toUpperCase() === 'ACTIVO' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-700'">{{ vehicleDetail.estatus }}</span>
              </div>
              <dl class="mt-5 grid gap-4 text-sm sm:grid-cols-2 lg:grid-cols-3"><div><dt class="text-slate-500">Cliente</dt><dd class="font-semibold text-slate-800">{{ vehicleDetail.cliente.nombreCompleto }}</dd></div><div><dt class="text-slate-500">Taller</dt><dd class="font-semibold text-slate-800">{{ vehicleDetail.taller?.nombreTaller || 'Sin taller asignado' }}</dd></div><div><dt class="text-slate-500">Kilometraje</dt><dd class="font-semibold text-slate-800">{{ vehicleDetail.kilometraje }}</dd></div><div><dt class="text-slate-500">Color</dt><dd class="font-semibold text-slate-800">{{ vehicleDetail.color }}</dd></div><div><dt class="text-slate-500">Combustible</dt><dd class="font-semibold text-slate-800">{{ vehicleDetail.tipoCombustible }}</dd></div><div><dt class="text-slate-500">Transmision</dt><dd class="font-semibold text-slate-800">{{ vehicleDetail.tipoTransmision }}</dd></div></dl>
              <div class="mt-6 flex flex-wrap gap-3"><button v-if="canRegisterCustomers" type="button" class="rounded-lg border border-rose-200 bg-rose-50 px-4 py-2 font-semibold text-rose-800 hover:bg-rose-100 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="openVehicleEditModal(vehicleDetail)">Editar</button><button v-if="isAdmin" type="button" class="rounded-lg border border-slate-300 px-4 py-2 font-semibold text-slate-800 hover:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="openVehicleStatusModal(vehicleDetail)">Estado</button></div>
            </article>
          </section>

          <section v-if="activeView === 'estatus' && isAdmin" class="rounded-xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <div><p class="text-xs font-bold uppercase tracking-[0.16em] text-rose-400">Catalogo</p><h2 class="mt-1 text-2xl font-bold text-slate-900">Administracion de estatus</h2></div>
            <form class="mt-5 grid gap-3 sm:grid-cols-[1fr_2fr_auto]" @submit.prevent="saveStatus"><input v-model.trim="statusDraft.strValor" placeholder="Valor del estatus" class="rounded-lg border border-slate-200 px-3 py-2.5" required /><input v-model.trim="statusDraft.strDescripcion" placeholder="Descripcion opcional" class="rounded-lg border border-slate-200 px-3 py-2.5" /><button class="rounded-lg bg-rose-600 px-4 py-2 font-bold text-white">{{ statusDraft.id ? 'Actualizar' : 'Registrar' }}</button></form>
            <div class="mt-5 overflow-x-auto"><table class="min-w-full text-left text-sm"><thead class="border-b border-slate-200 text-xs uppercase text-slate-500"><tr><th class="p-3">Estatus</th><th class="p-3">Descripcion</th><th class="p-3">Activo</th><th class="p-3">Acciones</th></tr></thead><tbody><tr v-for="status in statuses" :key="status.id" class="border-b border-slate-100"><td class="p-3 font-semibold">{{ status.strValor }}</td><td class="p-3 text-slate-600">{{ status.strDescripcion }}</td><td class="p-3">{{ status.activo ? 'Si' : 'No' }}</td><td class="p-3"><button class="mr-3 text-rose-700" @click="editStatus(status)">Editar</button><button v-if="status.activo" class="text-slate-600" @click="deactivateStatus(status)">Desactivar</button></td></tr></tbody></table></div>
          </section>

          <div v-if="vehicleModalCustomer" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
            <form class="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-xl bg-white p-6 shadow-xl" @submit.prevent="submitVehicle">
              <div class="flex items-start justify-between gap-4"><div><h2 class="text-xl font-bold text-slate-900">Añadir vehiculo</h2><p class="text-sm text-slate-500">Cliente: {{ vehicleModalCustomer.fullName }}</p></div><button type="button" class="text-sm font-semibold text-slate-500 hover:text-slate-900" @click="vehicleModalCustomer = null">Cerrar</button></div>
              <div class="mt-5 grid gap-4 sm:grid-cols-2">
                <label class="text-sm font-semibold">VIN<input v-model.trim="vehicleForm.vin" maxlength="17" class="mt-1 w-full rounded-lg border p-2 uppercase" required /></label><label class="text-sm font-semibold">Placa<input v-model.trim="vehicleForm.placa" class="mt-1 w-full rounded-lg border p-2 uppercase" required /></label>
                <label class="text-sm font-semibold">Marca<select v-model="vehicleForm.marcaId" class="mt-1 w-full rounded-lg border p-2" @change="loadVehicleModels"><option value="">Capturar nueva marca</option><option v-for="brand in vehicleBrands" :key="brand.id" :value="String(brand.id)">{{ brand.nombre }}</option></select></label><label v-if="!vehicleForm.marcaId" class="text-sm font-semibold">Nueva marca<input v-model.trim="vehicleForm.nuevaMarca" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Modelo<select v-model="vehicleForm.modeloId" class="mt-1 w-full rounded-lg border p-2" @change="applyVehicleModel"><option value="">Capturar nuevo modelo</option><option v-for="model in vehicleModels" :key="model.id" :value="String(model.id)">{{ model.nombre }}</option></select></label><label v-if="!vehicleForm.modeloId" class="text-sm font-semibold">Nuevo modelo<input v-model.trim="vehicleForm.nuevoModelo" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Año<input v-model="vehicleForm.anio" type="number" min="1900" max="2100" class="mt-1 w-full rounded-lg border p-2" required /></label><label class="text-sm font-semibold">Version<input v-model.trim="vehicleForm.version" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Color<input v-model.trim="vehicleForm.color" class="mt-1 w-full rounded-lg border p-2" required /></label><label class="text-sm font-semibold">Numero de serie<input v-model.trim="vehicleForm.numeroSerie" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Kilometraje<input v-model="vehicleForm.kilometraje" type="number" min="0" class="mt-1 w-full rounded-lg border p-2" required /></label><label class="text-sm font-semibold">Combustible<input v-model.trim="vehicleForm.tipoCombustible" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Cilindros<input v-model.trim="vehicleForm.numeroCilindros" class="mt-1 w-full rounded-lg border p-2" required /></label><label class="text-sm font-semibold">Transmision<input v-model.trim="vehicleForm.tipoTransmision" class="mt-1 w-full rounded-lg border p-2" required /></label>
              </div>
              <button class="mt-6 w-full rounded-lg bg-rose-600 px-4 py-3 font-bold text-white">Registrar vehiculo</button>
            </form>
          </div>

          <div v-if="vehicleEditModal" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
            <form class="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-xl bg-white p-6 shadow-xl" @submit.prevent="submitVehicleUpdate">
              <div class="flex items-start justify-between gap-4"><div><h2 class="text-xl font-bold text-slate-900">Editar vehiculo</h2><p class="text-sm text-slate-500">VIN inmutable: {{ vehicleEditModal.vin }}</p></div><button type="button" class="text-sm font-semibold text-slate-500 hover:text-slate-900" @click="vehicleEditModal = null">Cerrar</button></div>
              <div class="mt-5 grid gap-4 sm:grid-cols-2">
                <label class="text-sm font-semibold">VIN<input :value="vehicleEditModal.vin" disabled class="mt-1 w-full rounded-lg border bg-slate-100 p-2 text-slate-500" /></label><label class="text-sm font-semibold">Placa<input v-model.trim="vehicleEditForm.placa" class="mt-1 w-full rounded-lg border p-2 uppercase" required /></label>
                <label class="text-sm font-semibold">Marca<select v-model="vehicleEditForm.marcaId" class="mt-1 w-full rounded-lg border p-2" @change="loadVehicleEditModels"><option value="">Capturar nueva marca</option><option v-for="brand in vehicleBrands" :key="brand.id" :value="String(brand.id)">{{ brand.nombre }}</option></select></label><label v-if="!vehicleEditForm.marcaId" class="text-sm font-semibold">Nueva marca<input v-model.trim="vehicleEditForm.nuevaMarca" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Modelo<select v-model="vehicleEditForm.modeloId" class="mt-1 w-full rounded-lg border p-2" @change="applyVehicleEditModel"><option value="">Capturar nuevo modelo</option><option v-for="model in vehicleEditModels" :key="model.id" :value="String(model.id)">{{ model.nombre }}</option></select></label><label v-if="!vehicleEditForm.modeloId" class="text-sm font-semibold">Nuevo modelo<input v-model.trim="vehicleEditForm.nuevoModelo" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Año<input v-model="vehicleEditForm.anio" type="number" min="1900" max="2100" class="mt-1 w-full rounded-lg border p-2" required /></label><label class="text-sm font-semibold">Version<input v-model.trim="vehicleEditForm.version" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Color<input v-model.trim="vehicleEditForm.color" class="mt-1 w-full rounded-lg border p-2" required /></label><label class="text-sm font-semibold">Numero de serie<input v-model.trim="vehicleEditForm.numeroSerie" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Kilometraje<input v-model="vehicleEditForm.kilometraje" type="number" min="0" class="mt-1 w-full rounded-lg border p-2" required /></label><label class="text-sm font-semibold">Combustible<input v-model.trim="vehicleEditForm.tipoCombustible" class="mt-1 w-full rounded-lg border p-2" required /></label>
                <label class="text-sm font-semibold">Cilindros<input v-model.trim="vehicleEditForm.numeroCilindros" class="mt-1 w-full rounded-lg border p-2" required /></label><label class="text-sm font-semibold">Transmision<input v-model.trim="vehicleEditForm.tipoTransmision" class="mt-1 w-full rounded-lg border p-2" required /></label>
              </div>
              <button class="mt-6 w-full rounded-lg bg-rose-600 px-4 py-3 font-bold text-white">Guardar cambios</button>
            </form>
          </div>

          <div v-if="vehicleStatusModal" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
            <form class="w-full max-w-md rounded-xl bg-white p-6 shadow-xl" @submit.prevent="updateVehicleStatus">
              <h2 class="text-xl font-bold text-slate-900">Cambiar estado del vehiculo</h2><p class="mt-1 text-sm text-slate-500">{{ vehicleStatusModal.vin }} · {{ vehicleStatusModal.placa }}</p>
              <label class="mt-5 block text-sm font-semibold text-slate-700">Estado<select v-model="selectedVehicleStatusId" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required><option disabled value="">Selecciona un estatus</option><option v-for="status in statuses.filter(status => status.activo)" :key="status.id" :value="String(status.id)">{{ status.strValor }}</option></select></label>
              <label class="mt-5 flex items-start gap-3 text-sm text-slate-700"><input v-model="confirmVehicleStatusChange" type="checkbox" class="mt-1 h-4 w-4 rounded border-slate-300 text-rose-600 focus:ring-rose-400" required /><span>Confirmo que deseo aplicar este cambio de estado. La operacion quedara auditada.</span></label>
              <div class="mt-6 flex justify-end gap-3"><button type="button" class="rounded-lg border border-slate-300 px-4 py-2 font-semibold text-slate-700" @click="vehicleStatusModal = null">Cancelar</button><button class="rounded-lg bg-rose-600 px-4 py-2 font-bold text-white">Confirmar estado</button></div>
            </form>
          </div>

          <div v-if="customerStatusModal" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
            <form class="w-full max-w-md rounded-xl bg-white p-6 shadow-xl" @submit.prevent="updateCustomerStatus">
              <h2 class="text-xl font-bold text-slate-900">Cambiar estado</h2>
              <p class="mt-1 text-sm text-slate-500">{{ customerStatusModal.fullName }}</p>
              <label class="mt-5 block text-sm font-semibold text-slate-700">Estado
                <select v-model="selectedCustomerStatusId" class="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required>
                  <option disabled value="">Selecciona un estatus</option>
                  <option v-for="status in statuses.filter(status => status.activo)" :key="status.id" :value="String(status.id)">{{ status.strValor }}</option>
                </select>
              </label>
              <div class="mt-6 flex justify-end gap-3"><button type="button" class="rounded-lg border border-slate-300 px-4 py-2 font-semibold text-slate-700" @click="customerStatusModal = null">Cancelar</button><button class="rounded-lg bg-rose-600 px-4 py-2 font-bold text-white">Guardar estado</button></div>
            </form>
          </div>

          <template v-if="false">
            <section><h2>Modelo de seguridad</h2></section>
            <section><h2>Panel interactivo</h2></section>
            <section><h2>Administracion de roles</h2><button @click="loadRoles">Actualizar</button><form @submit.prevent="createRole"><input v-model="roleDraft.name" /><input v-model="roleDraft.description" /><input v-model="roleDraft.permissions" /><button>Crear</button></form><article v-for="role in roles" :key="role.id">{{ role.name }}</article></section>
          </template>
        </div>
      </div>
    </section>
  </main>
</template>
