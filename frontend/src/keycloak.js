import Keycloak from 'keycloak-js'

// Cliente publico de Keycloak. Se usa el flujo Authorization Code + PKCE:
// el usuario se loguea en la pantalla de Keycloak (el front nunca ve la contraseña)
// y Keycloak nos devuelve el JWT con los roles.
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:9090',
  realm: 'depositoclick',
  clientId: 'depositoclick-client'
})

export default keycloak

export function rolesDelUsuario() {
  return keycloak.tokenParsed?.realm_access?.roles || []
}

export function tieneRol(...roles) {
  const mios = rolesDelUsuario()
  return roles.some(r => mios.includes(r))
}
