import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import { tieneRol } from './keycloak'
import Inicio from './pages/Inicio'
import Catalogo from './pages/Catalogo'
import PuntoDeVenta from './pages/PuntoDeVenta'
import Stock from './pages/Stock'
import Movimientos from './pages/Movimientos'
import Ordenes from './pages/Ordenes'
import OrdenDetalle from './pages/OrdenDetalle'
import NuevaOrden from './pages/NuevaOrden'
import Reposicion from './pages/Reposicion'

// Solo deja entrar a la pagina si el usuario tiene alguno de los roles.
// (Es solo para la interfaz: la seguridad real la hacen el gateway y los microservicios)
function ConRol({ roles, children }) {
  return tieneRol(...roles) ? children : <Navigate to="/" replace />
}

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Inicio />} />
        <Route path="/catalogo" element={<Catalogo />} />
        <Route path="/stock" element={<Stock />} />
        <Route path="/venta" element={
          <ConRol roles={['VENDEDOR', 'OPERARIO', 'ADMIN']}><PuntoDeVenta /></ConRol>} />
        <Route path="/movimientos" element={
          <ConRol roles={['OPERARIO', 'ADMIN']}><Movimientos /></ConRol>} />
        <Route path="/ordenes" element={
          <ConRol roles={['COMPRAS', 'OPERARIO', 'ADMIN']}><Ordenes /></ConRol>} />
        <Route path="/ordenes/nueva" element={
          <ConRol roles={['COMPRAS', 'ADMIN']}><NuevaOrden /></ConRol>} />
        <Route path="/ordenes/:id" element={
          <ConRol roles={['COMPRAS', 'OPERARIO', 'ADMIN']}><OrdenDetalle /></ConRol>} />
        <Route path="/reposicion" element={
          <ConRol roles={['COMPRAS', 'ADMIN']}><Reposicion /></ConRol>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  )
}
