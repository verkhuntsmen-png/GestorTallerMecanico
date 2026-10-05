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
const customerPhotoInput = ref(null)
const customerPhotoPreview = ref('')
const sepomexStates = ref([])
const sepomexMunicipalities = ref([])
const sepomexNeighborhoods = ref([])
const selectedStateId = ref('')
const selectedMunicipalityId = ref('')
const addressLoading = ref(false)
const addressNotice = ref('')
const roleDraft = reactive({ name: '', description: '', permissions: 'ORDERS_READ, ORDERS_WRITE' })
const loginForm = reactive({ email: 'admin@taller.local', password: 'Admin123!Seguro' })
const registerForm = reactive({ fullName: '', email: '', password: '' })
const forgotForm = reactive({ email: '' })
const resetForm = reactive({ token: '', newPassword: '' })
const customerForm = reactive({
  fullName: '', alternateFullName: '', personalPhone: '', workPhone: '', age: '', birthDate: '', email: '',
  workEmail: '', photo: null, street: '', neighborhood: '', municipality: '', state: '', postalCode: ''
})

const isAdmin = computed(() => user.value?.roles?.some((role) => role.permissions?.includes('SECURITY_MANAGE_ROLES')))
const canRegisterCustomers = computed(() => user.value?.roles?.some((role) => role.permissions?.includes('CLIENTS_WRITE')))

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
  } catch {
    logout()
  }
}

async function loadRoles() { roles.value = await api('/roles') }

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
  customerForm.photo = photo
  customerPhotoPreview.value = URL.createObjectURL(photo)
}

async function submitCustomer() {
  await run(async () => {
    const formData = new FormData()
    Object.entries(customerForm).forEach(([key, value]) => { if (value !== null && value !== '') formData.append(key, value) })
    const customer = await api('/customers', { method: 'POST', body: formData })
    Object.assign(customerForm, {
      fullName: '', alternateFullName: '', personalPhone: '', workPhone: '', age: '', birthDate: '', email: '',
      workEmail: '', photo: null, street: '', neighborhood: '', municipality: '', state: '', postalCode: ''
    })
    clearCustomerPhoto()
    success.value = `Nuevo cliente creado: ${customer.fullName}`
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
  localStorage.removeItem('auth_token')
  mode.value = 'login'
}

watch(() => customerForm.postalCode, applyZipCode)
onMounted(async () => {
  await Promise.all([loadMe(), loadStates()])
})
</script>

<template>
  <main class="min-h-screen bg-slate-950 font-sans text-slate-100">
    <header class="border-b border-slate-800 bg-slate-900/90">
      <div class="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-5 sm:px-6 lg:px-8">
        <div>
          <p class="text-xs font-bold uppercase tracking-[0.18em] text-rose-400">Taller mecanico</p>
          <h1 class="mt-1 text-2xl font-bold tracking-tight text-slate-50">Refaccionaria</h1>
        </div>
        <button v-if="user" class="rounded-lg border border-rose-500/20 bg-slate-800 px-4 py-2 text-sm font-semibold text-slate-100 transition hover:border-rose-400 hover:bg-rose-500/10 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="logout">
          Cerrar sesion
        </button>
      </div>
    </header>

    <section class="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-10" :class="user ? '' : 'max-w-lg'">
      <div class="grid gap-6" :class="user ? 'lg:grid-cols-[300px_minmax(0,1fr)]' : ''">
        <aside class="rounded-xl border border-slate-700 bg-slate-900 p-5 shadow-2xl shadow-slate-950/20">
          <div v-if="!user" class="space-y-5">
            <div class="grid grid-cols-2 rounded-lg border border-slate-700 bg-slate-950 p-1">
              <button class="rounded-md px-3 py-2 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="mode === 'login' ? 'bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 text-white' : 'text-slate-400 hover:text-rose-200'" @click="mode = 'login'">Login</button>
              <button class="rounded-md px-3 py-2 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-rose-400" :class="mode === 'register' ? 'bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 text-white' : 'text-slate-400 hover:text-rose-200'" @click="mode = 'register'">Registro</button>
            </div>

            <form v-if="mode === 'login'" class="space-y-4" @submit.prevent="submitLogin">
              <h2 class="text-xl font-bold text-slate-100">Iniciar sesion</h2>
              <label class="block text-sm font-semibold text-slate-300">Correo<input v-model="loginForm.email" type="email" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-100 placeholder-slate-500 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <label class="block text-sm font-semibold text-slate-300">Contrasena<input v-model="loginForm.password" type="password" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <button class="w-full rounded-lg bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 px-4 py-2.5 font-bold text-white transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-rose-400 disabled:opacity-60" :disabled="loading">Entrar</button>
              <button type="button" class="text-sm font-semibold text-rose-300 hover:text-rose-200 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="mode = 'forgot'">Recuperar contrasena</button>
            </form>

            <form v-if="mode === 'register'" class="space-y-4" @submit.prevent="submitRegister">
              <h2 class="text-xl font-bold text-slate-100">Crear usuario cliente</h2>
              <label class="block text-sm font-semibold text-slate-300">Nombre completo<input v-model="registerForm.fullName" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <label class="block text-sm font-semibold text-slate-300">Correo<input v-model="registerForm.email" type="email" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <label class="block text-sm font-semibold text-slate-300">Contrasena segura<input v-model="registerForm.password" type="password" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <button class="w-full rounded-lg bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 px-4 py-2.5 font-bold text-white transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-rose-400 disabled:opacity-60" :disabled="loading">Registrar</button>
            </form>

            <form v-if="mode === 'forgot'" class="space-y-4" @submit.prevent="submitForgot">
              <h2 class="text-xl font-bold text-slate-100">Recuperar contrasena</h2>
              <label class="block text-sm font-semibold text-slate-300">Correo registrado<input v-model="forgotForm.email" type="email" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <button class="w-full rounded-lg bg-amber-400 px-4 py-2.5 font-bold text-slate-950 transition hover:bg-amber-300 focus:outline-none focus:ring-2 focus:ring-rose-400" :disabled="loading">Generar token</button>
              <button type="button" class="text-sm font-semibold text-rose-300 hover:text-rose-200 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="mode = 'reset'">Ya tengo token</button>
            </form>

            <form v-if="mode === 'reset'" class="space-y-4" @submit.prevent="submitReset">
              <h2 class="text-xl font-bold text-slate-100">Restablecer contrasena</h2>
              <label class="block text-sm font-semibold text-slate-300">Token<input v-model="resetForm.token" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <label class="block text-sm font-semibold text-slate-300">Nueva contrasena<input v-model="resetForm.newPassword" type="password" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
              <button class="w-full rounded-lg bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 px-4 py-2.5 font-bold text-white transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-rose-400" :disabled="loading">Actualizar</button>
            </form>
          </div>

          <div v-else class="space-y-4">
            <p class="text-xs font-bold uppercase tracking-[0.16em] text-slate-500">Sesion activa</p>
            <div class="rounded-lg border border-slate-700 bg-slate-950 p-4">
              <p class="font-bold text-slate-100">{{ user.fullName }}</p><p class="mt-1 text-sm text-slate-400">{{ user.email }}</p>
            </div>
            <div class="flex flex-wrap gap-2"><span v-for="role in user.roles" :key="role.id" class="rounded-md border border-rose-400/30 bg-rose-500/10 px-2.5 py-1 text-xs font-bold text-rose-200">{{ role.name }}</span></div>
          </div>

          <div v-if="error" class="mt-4 rounded-lg border border-rose-400/40 bg-rose-400/10 px-3 py-2 text-sm font-semibold text-rose-200" role="alert">{{ error }}</div>
          <div v-if="success" class="mt-4 rounded-lg border border-emerald-400/40 bg-emerald-400/10 px-3 py-2 text-sm font-semibold text-emerald-200" role="status">{{ success }}</div>
          <div v-if="resetToken" class="mt-3 break-all rounded-lg border border-amber-400/30 bg-amber-400/10 px-3 py-2 text-xs font-semibold text-amber-100">Token dev: {{ resetToken }}</div>
        </aside>

        <div v-if="user" class="space-y-6">
          <section class="rounded-xl border border-slate-700 bg-slate-900 p-5 shadow-xl shadow-slate-950/20 sm:p-6">
            <p class="text-sm text-slate-400">Inicio</p><h2 class="mt-1 text-2xl font-bold text-slate-100">Operacion del taller</h2>
            <p class="mt-2 max-w-2xl text-sm leading-6 text-slate-400">Gestiona clientes con una vista concentrada, responsive y conectada a los servicios autorizados de la plataforma.</p>
          </section>

          <section v-if="canRegisterCustomers" class="rounded-xl border border-slate-700 bg-slate-900 p-5 shadow-xl shadow-slate-950/20 sm:p-6">
            <div class="flex flex-wrap items-start justify-between gap-3"><div><p class="text-xs font-bold uppercase tracking-[0.16em] text-rose-400">Clientes</p><h2 class="mt-1 text-2xl font-bold text-slate-100">Registrar nuevo cliente</h2></div><span class="rounded-full border border-rose-400/30 bg-rose-500/10 px-3 py-1 text-xs font-bold text-rose-200">Acceso autorizado</span></div>

            <form class="mt-6 space-y-6" @submit.prevent="submitCustomer">
              <fieldset class="rounded-xl border border-slate-700 bg-slate-950/50 p-4 shadow-sm sm:p-5"><legend class="px-2 text-sm font-bold text-slate-200">Informacion personal</legend>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <label class="text-sm font-semibold text-slate-300">Nombre completo<input v-model.trim="customerForm.fullName" pattern="[A-Za-zÁÉÍÓÚáéíóúÑñ ]+" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-300">Nombre alternativo<input v-model.trim="customerForm.alternateFullName" pattern="[A-Za-zÁÉÍÓÚáéíóúÑñ ]+" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-300">Edad<input v-model.number="customerForm.age" type="number" min="0" max="130" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-300">Fecha de nacimiento<input v-model="customerForm.birthDate" type="date" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                </div>
              </fieldset>

              <fieldset class="rounded-xl border border-slate-700 bg-slate-950/50 p-4 shadow-sm sm:p-5"><legend class="px-2 text-sm font-bold text-slate-200">Contacto y fotografia</legend>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <label class="text-sm font-semibold text-slate-300">Telefono personal<input v-model.trim="customerForm.personalPhone" type="tel" placeholder="+525512345678" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 placeholder-slate-500 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-300">Telefono trabajo<input v-model.trim="customerForm.workPhone" type="tel" placeholder="+525512345678" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 placeholder-slate-500 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-300">Email principal<input v-model.trim="customerForm.email" type="email" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-300">Email trabajo<input v-model.trim="customerForm.workEmail" type="email" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" /></label>
                  <div class="sm:col-span-2">
                    <input ref="customerPhotoInput" type="file" accept="image/jpeg,image/png,image/webp" class="sr-only" required @change="selectCustomerPhoto" />
                    <div class="flex min-h-44 flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-600 bg-slate-900/60 p-4 text-center transition hover:border-rose-400" @dragover.prevent @drop.prevent="selectCustomerPhoto">
                      <img v-if="customerPhotoPreview" :src="customerPhotoPreview" alt="Previsualizacion de fotografia" class="h-28 w-28 rounded-lg object-cover" />
                      <p class="mt-3 text-sm font-bold text-slate-200">{{ customerPhotoPreview ? 'Fotografia seleccionada' : 'Arrastra una fotografia aqui' }}</p>
                      <p class="mt-1 text-xs text-slate-400">JPEG, PNG o WEBP. Maximo 15 MB.</p>
                      <div class="mt-3 flex gap-2"><button type="button" class="rounded-lg border border-slate-600 px-3 py-2 text-sm font-semibold text-slate-200 hover:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="customerPhotoInput?.click()">Seleccionar archivo</button><button v-if="customerPhotoPreview" type="button" class="rounded-lg px-3 py-2 text-sm font-semibold text-slate-400 hover:text-slate-100 focus:outline-none focus:ring-2 focus:ring-rose-400" @click="clearCustomerPhoto">Quitar</button></div>
                    </div>
                  </div>
                </div>
              </fieldset>

              <fieldset class="rounded-xl border border-rose-500/20 bg-slate-950/50 p-4 shadow-sm sm:p-5"><legend class="px-2 text-sm font-bold text-slate-200">Direccion completa</legend>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  <label class="text-sm font-semibold text-slate-300">Codigo postal<input v-model.trim="customerForm.postalCode" inputmode="numeric" pattern="[0-9]{5}" maxlength="5" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label v-if="sepomexStates.length" class="text-sm font-semibold text-slate-300">Estado<select v-model="selectedStateId" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required @change="selectState"><option disabled value="">Selecciona un estado</option><option v-for="state in sepomexStates" :key="state.id" :value="String(state.id)">{{ state.name }}</option></select></label>
                  <label v-else class="text-sm font-semibold text-slate-300">Estado<input v-model.trim="customerForm.state" pattern="[A-Za-zÁÉÍÓÚáéíóúÑñ ]+" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label v-if="sepomexMunicipalities.length" class="text-sm font-semibold text-slate-300">Municipio o alcaldia<select v-model="selectedMunicipalityId" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required @change="selectMunicipality"><option disabled value="">Selecciona un municipio</option><option v-for="municipality in sepomexMunicipalities" :key="municipality.id" :value="String(municipality.id)">{{ municipality.name }}</option></select></label>
                  <label v-else class="text-sm font-semibold text-slate-300">Municipio o alcaldia<input v-model.trim="customerForm.municipality" pattern="[A-Za-zÁÉÍÓÚáéíóúÑñ ]+" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label v-if="sepomexNeighborhoods.length" class="text-sm font-semibold text-slate-300">Colonia<select v-model="customerForm.neighborhood" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required><option disabled value="">Selecciona una colonia</option><option v-for="neighborhood in sepomexNeighborhoods" :key="neighborhood.id" :value="neighborhood.d_asenta">{{ neighborhood.d_asenta }}</option></select></label>
                  <label v-else class="text-sm font-semibold text-slate-300">Colonia<input v-model.trim="customerForm.neighborhood" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                  <label class="text-sm font-semibold text-slate-300 lg:col-span-2">Calle<input v-model.trim="customerForm.street" class="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2.5 text-slate-100 focus:border-rose-400 focus:outline-none focus:ring-2 focus:ring-rose-400" required /></label>
                </div>
                <p v-if="addressLoading" class="mt-3 text-sm font-semibold text-amber-300">Consultando catalogo de direcciones...</p>
                <p v-else-if="addressNotice" class="mt-3 text-sm text-rose-200" role="status">{{ addressNotice }}</p>
              </fieldset>
              <button class="w-full rounded-lg bg-gradient-to-r from-rose-500/80 via-fuchsia-500/80 to-amber-500/80 px-4 py-3 font-bold text-white transition hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-rose-400 disabled:opacity-60" :disabled="loading">Registrar cliente</button>
            </form>
          </section>

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
