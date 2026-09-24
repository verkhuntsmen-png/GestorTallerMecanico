<script setup>
import { computed, onMounted, reactive, ref } from 'vue'

const API_BASE = '/api'
const token = ref(localStorage.getItem('auth_token') || '')
const user = ref(null)
const mode = ref('login')
const loading = ref(false)
const error = ref('')
const success = ref('')
const resetToken = ref('')
const roles = ref([])
const roleDraft = reactive({
  name: '',
  description: '',
  permissions: 'ORDERS_READ, ORDERS_WRITE'
})

const loginForm = reactive({
  email: 'admin@taller.local',
  password: 'Admin123!Seguro'
})

const registerForm = reactive({
  fullName: '',
  email: '',
  password: ''
})

const forgotForm = reactive({ email: '' })
const resetForm = reactive({ token: '', newPassword: '' })

const isAdmin = computed(() =>
  user.value?.roles?.some((role) => role.permissions?.includes('SECURITY_MANAGE_ROLES'))
)

async function api(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }
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
    const response = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify(loginForm)
    })
    setAuth(response)
    success.value = 'Sesion iniciada correctamente'
  })
}

async function submitRegister() {
  await run(async () => {
    const response = await api('/auth/register', {
      method: 'POST',
      body: JSON.stringify(registerForm)
    })
    setAuth(response)
    success.value = 'Cuenta creada con rol CLIENTE'
  })
}

async function submitForgot() {
  await run(async () => {
    const response = await api('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify(forgotForm)
    })
    resetToken.value = response.resetToken || ''
    resetForm.token = response.resetToken || ''
    success.value = response.message
  })
}

async function submitReset() {
  await run(async () => {
    const response = await api('/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify(resetForm)
    })
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

async function loadRoles() {
  roles.value = await api('/roles')
}

async function createRole() {
  await run(async () => {
    const permissions = roleDraft.permissions
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)

    await api('/roles', {
      method: 'POST',
      body: JSON.stringify({
        name: roleDraft.name,
        description: roleDraft.description,
        permissions
      })
    })
    roleDraft.name = ''
    roleDraft.description = ''
    roleDraft.permissions = 'ORDERS_READ'
    await loadRoles()
    success.value = 'Rol creado correctamente'
  })
}

async function run(action) {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await action()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

function logout() {
  token.value = ''
  user.value = null
  roles.value = []
  localStorage.removeItem('auth_token')
  mode.value = 'login'
}

onMounted(loadMe)
</script>

<template>
  <main class="min-h-screen bg-[#f4f6f2] text-[#253128]">
    <section class="border-b border-[#d7ddd0] bg-[#20372b] text-white">
      <div class="mx-auto flex max-w-6xl flex-col gap-4 px-5 py-6 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p class="text-sm font-semibold uppercase tracking-wide text-[#f28c28]">Taller mecanico</p>
          <h1 class="mt-1 text-3xl font-bold">Refaccionaria: Acceso seguro</h1>
        </div>
        <button
          v-if="user"
          class="rounded-md bg-[#f28c28] px-4 py-2 font-semibold text-[#1f2c23] transition hover:bg-[#ff9d3e]"
          @click="logout"
        >
          Cerrar sesion
        </button>
      </div>
    </section>

    <section class="mx-auto grid max-w-6xl gap-6 px-5 py-8 lg:grid-cols-[420px_1fr]">
      <div class="rounded-lg border border-[#d7ddd0] bg-white p-5 shadow-sm">
        <div v-if="!user" class="space-y-5">
          <div class="grid grid-cols-2 gap-2 rounded-md bg-[#edf1ea] p-1">
            <button
              class="rounded-md px-3 py-2 text-sm font-semibold"
              :class="mode === 'login' ? 'bg-[#2f6b44] text-white' : 'text-[#47564b]'"
              @click="mode = 'login'"
            >
              Login
            </button>
            <button
              class="rounded-md px-3 py-2 text-sm font-semibold"
              :class="mode === 'register' ? 'bg-[#2f6b44] text-white' : 'text-[#47564b]'"
              @click="mode = 'register'"
            >
              Registro
            </button>
          </div>

          <form v-if="mode === 'login'" class="space-y-4" @submit.prevent="submitLogin">
            <h2 class="text-xl font-bold">Iniciar sesion</h2>
            <label class="block text-sm font-semibold">
              Correo
              <input v-model="loginForm.email" type="email" class="mt-1 w-full rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            </label>
            <label class="block text-sm font-semibold">
              Contrasena
              <input v-model="loginForm.password" type="password" class="mt-1 w-full rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            </label>
            <button class="w-full rounded-md bg-[#2f6b44] px-4 py-2 font-bold text-white transition hover:bg-[#265839]" :disabled="loading">
              Entrar
            </button>
            <button type="button" class="text-sm font-semibold text-[#c76616]" @click="mode = 'forgot'">
              Recuperar contrasena
            </button>
          </form>

          <form v-if="mode === 'register'" class="space-y-4" @submit.prevent="submitRegister">
            <h2 class="text-xl font-bold">Crear usuario cliente</h2>
            <label class="block text-sm font-semibold">
              Nombre completo
              <input v-model="registerForm.fullName" class="mt-1 w-full rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            </label>
            <label class="block text-sm font-semibold">
              Correo
              <input v-model="registerForm.email" type="email" class="mt-1 w-full rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            </label>
            <label class="block text-sm font-semibold">
              Contrasena segura
              <input v-model="registerForm.password" type="password" class="mt-1 w-full rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            </label>
            <button class="w-full rounded-md bg-[#2f6b44] px-4 py-2 font-bold text-white transition hover:bg-[#265839]" :disabled="loading">
              Registrar
            </button>
          </form>

          <form v-if="mode === 'forgot'" class="space-y-4" @submit.prevent="submitForgot">
            <h2 class="text-xl font-bold">Recuperar contrasena</h2>
            <label class="block text-sm font-semibold">
              Correo registrado
              <input v-model="forgotForm.email" type="email" class="mt-1 w-full rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            </label>
            <button class="w-full rounded-md bg-[#f28c28] px-4 py-2 font-bold text-[#1f2c23]" :disabled="loading">
              Generar token
            </button>
            <button type="button" class="text-sm font-semibold text-[#2f6b44]" @click="mode = 'reset'">
              Ya tengo token
            </button>
          </form>

          <form v-if="mode === 'reset'" class="space-y-4" @submit.prevent="submitReset">
            <h2 class="text-xl font-bold">Restablecer contrasena</h2>
            <label class="block text-sm font-semibold">
              Token
              <input v-model="resetForm.token" class="mt-1 w-full rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            </label>
            <label class="block text-sm font-semibold">
              Nueva contrasena
              <input v-model="resetForm.newPassword" type="password" class="mt-1 w-full rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            </label>
            <button class="w-full rounded-md bg-[#2f6b44] px-4 py-2 font-bold text-white" :disabled="loading">
              Actualizar
            </button>
          </form>
        </div>

        <div v-else class="space-y-4">
          <h2 class="text-xl font-bold">Sesion activa</h2>
          <div class="rounded-md bg-[#edf1ea] p-4">
            <p class="font-bold">{{ user.fullName }}</p>
            <p class="text-sm text-[#647064]">{{ user.email }}</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <span v-for="role in user.roles" :key="role.id" class="rounded-md bg-[#f28c28]/15 px-3 py-1 text-sm font-bold text-[#a34f0d]">
              {{ role.name }}
            </span>
          </div>
        </div>

        <div v-if="error" class="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm font-semibold text-red-700">
          {{ error }}
        </div>
        <div v-if="success" class="mt-4 rounded-md border border-[#b7d5bf] bg-[#eef8f0] px-3 py-2 text-sm font-semibold text-[#2f6b44]">
          {{ success }}
        </div>
        <div v-if="resetToken" class="mt-3 break-all rounded-md bg-[#fff4e8] px-3 py-2 text-xs font-semibold text-[#8d4a12]">
          Token dev: {{ resetToken }}
        </div>
      </div>

      <div class="space-y-6">
        <section class="rounded-lg border border-[#d7ddd0] bg-white p-5 shadow-sm">
          <h2 class="text-xl font-bold">Modelo de seguridad</h2>
          <div class="mt-4 grid gap-3 md:grid-cols-3">
            <div class="rounded-md bg-[#edf1ea] p-4">
              <p class="font-bold text-[#2f6b44]">Autenticacion</p>
              <p class="mt-1 text-sm text-[#647064]">JWT firmado y contrasenas BCrypt con salt por usuario.</p>
            </div>
            <div class="rounded-md bg-[#fff4e8] p-4">
              <p class="font-bold text-[#c76616]">Antifuerza bruta</p>
              <p class="mt-1 text-sm text-[#647064]">Bloqueo temporal por usuario e IP tras intentos fallidos.</p>
            </div>
            <div class="rounded-md bg-[#f0f1ee] p-4">
              <p class="font-bold text-[#4f5a50]">SQL seguro</p>
              <p class="mt-1 text-sm text-[#647064]">Repositorios JPA y validacion para evitar SQL dinamico inseguro.</p>
            </div>
          </div>
        </section>

        <section v-if="user" class="rounded-lg border border-[#d7ddd0] bg-white p-5 shadow-sm">
          <h2 class="text-xl font-bold">Panel interactivo</h2>
          <p class="mt-2 text-[#647064]">Base preparada para ordenes de reparacion, clientes, mecanicos y gerencia.</p>
        </section>

        <section v-if="isAdmin" class="rounded-lg border border-[#d7ddd0] bg-white p-5 shadow-sm">
          <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <h2 class="text-xl font-bold">Administracion de roles</h2>
            <button class="rounded-md border border-[#2f6b44] px-3 py-2 text-sm font-bold text-[#2f6b44]" @click="loadRoles">
              Actualizar
            </button>
          </div>

          <form class="mt-4 grid gap-3 lg:grid-cols-[1fr_1fr_1.4fr_auto]" @submit.prevent="createRole">
            <input v-model="roleDraft.name" placeholder="Nombre del rol" class="rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            <input v-model="roleDraft.description" placeholder="Descripcion" class="rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" />
            <input v-model="roleDraft.permissions" placeholder="Permisos separados por coma" class="rounded-md border border-[#cbd4c4] px-3 py-2 outline-none focus:border-[#2f6b44]" required />
            <button class="rounded-md bg-[#f28c28] px-4 py-2 font-bold text-[#1f2c23]">
              Crear
            </button>
          </form>

          <div class="mt-5 grid gap-3 md:grid-cols-2">
            <article v-for="role in roles" :key="role.id" class="rounded-md border border-[#e0e5dc] p-4">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <h3 class="font-bold text-[#2f6b44]">{{ role.name }}</h3>
                  <p class="text-sm text-[#647064]">{{ role.description }}</p>
                </div>
                <span class="rounded bg-[#edf1ea] px-2 py-1 text-xs font-bold text-[#4f5a50]">#{{ role.id }}</span>
              </div>
              <div class="mt-3 flex flex-wrap gap-2">
                <span v-for="permission in role.permissions" :key="permission" class="rounded bg-[#f0f1ee] px-2 py-1 text-xs font-semibold text-[#4f5a50]">
                  {{ permission }}
                </span>
              </div>
            </article>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>
