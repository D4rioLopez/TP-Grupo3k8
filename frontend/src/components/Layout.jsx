import { NavLink } from 'react-router-dom'
import keycloak, { rolesDelUsuario, tieneRol } from '../keycloak'

// Cada opcion del menu indica que roles la pueden ver
const MENU = [
  { a: '/', texto: 'Inicio', roles: null },
  { a: '/venta', texto: 'Punto de venta', roles: ['VENDEDOR', 'OPERARIO', 'ADMIN'] },
  { a: '/stock', texto: 'Stock', roles: null },
  { a: '/catalogo', texto: 'Catalogo', roles: null },
  { a: '/movimientos', texto: 'Movimientos', roles: ['OPERARIO', 'ADMIN'] },
  { a: '/ordenes', texto: 'Ordenes de compra', roles: ['COMPRAS', 'OPERARIO', 'ADMIN'] },
  { a: '/reposicion', texto: 'Reposicion', roles: ['COMPRAS', 'ADMIN'] }
]

const ROLES_APP = ['ADMIN', 'COMPRAS', 'OPERARIO', 'VENDEDOR']

export default function Layout({ children }) {
  const usuario = keycloak.tokenParsed?.preferred_username
  const roles = rolesDelUsuario().filter(r => ROLES_APP.includes(r))

  return (
    <div className="app">
      <header className="barra">
        <div className="marca">DepositoClick</div>
        <nav>
          {MENU.filter(m => !m.roles || tieneRol(...m.roles)).map(m => (
            <NavLink key={m.a} to={m.a} end={m.a === '/'}>{m.texto}</NavLink>
          ))}
        </nav>
        <div className="usuario">
          <span>{usuario} <small>({roles.join(', ') || 'sin rol'})</small></span>
          <button className="secundario" onClick={() => keycloak.logout({ redirectUri: window.location.origin })}>
            Salir
          </button>
        </div>
      </header>
      <main className="contenido">
        {roles.length === 0
          ? <p className="mensaje error">Tu usuario no tiene ningun rol de DepositoClick asignado en Keycloak.</p>
          : children}
      </main>
    </div>
  )
}
