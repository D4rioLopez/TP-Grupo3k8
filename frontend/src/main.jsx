import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import keycloak from './keycloak'
import App from './App'
import './styles.css'

const root = ReactDOM.createRoot(document.getElementById('root'))
root.render(<p className="cargando">Conectando con Keycloak...</p>)

// login-required: si no hay sesion, Keycloak muestra su pantalla de login y vuelve a la app
keycloak
  .init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
  .then(() => {
    root.render(
      <BrowserRouter>
        <App />
      </BrowserRouter>
    )
  })
  .catch(() => {
    root.render(
      <p className="cargando error">
        No se pudo conectar con Keycloak (http://localhost:9090). Verifica que la solucion este levantada.
      </p>
    )
  })
