import keycloak from './keycloak'

// Todas las llamadas van al API Gateway (/api/v1/...) con el token de Keycloak.
// Antes de cada llamada se renueva el token si le quedan menos de 30 segundos.
export async function api(path, { method = 'GET', body } = {}) {
  try {
    await keycloak.updateToken(30)
  } catch {
    keycloak.login()
    return
  }

  const res = await fetch('/api/v1' + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + keycloak.token
    },
    body: body ? JSON.stringify(body) : undefined
  })

  const texto = await res.text()
  let datos = null
  try {
    datos = texto ? JSON.parse(texto) : null
  } catch {
    datos = null // por ejemplo una pagina de error de nginx
  }

  if (!res.ok) {
    // el backend siempre responde errores con el mismo formato (ErrorResponse)
    const error = new Error(datos?.mensaje || mensajePorStatus(res.status))
    error.status = res.status
    error.detalles = datos?.detalles
    throw error
  }
  return datos
}

function mensajePorStatus(status) {
  if (status === 401) return 'La sesion vencio. Volve a ingresar.'
  if (status === 403) return 'No tenes permisos para esta operacion.'
  if (status === 502 || status === 503 || status === 504) return 'El servicio no esta disponible. Reintenta en unos minutos.'
  return 'Error ' + status
}

// Convierte el error de la API en un texto para mostrar (incluye errores por campo)
export function textoError(e) {
  if (!e) return null
  if (e.detalles) {
    return e.message + ': ' + Object.entries(e.detalles).map(([k, v]) => `${k} (${v})`).join(', ')
  }
  return e.message
}
