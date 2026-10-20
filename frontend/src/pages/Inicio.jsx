import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, textoError } from '../api'
import keycloak, { tieneRol } from '../keycloak'
import Mensaje from '../components/Mensaje'
import { cantidad, fecha } from '../util'

export default function Inicio() {
  const [bajoMinimo, setBajoMinimo] = useState([])
  const [vencimientos, setVencimientos] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    Promise.all([api('/stock/bajo-minimo'), api('/stock/vencimientos?dias=30')])
      .then(([bm, v]) => {
        setBajoMinimo(bm)
        setVencimientos(v)
      })
      .catch(e => setError(textoError(e)))
  }, [])

  const nombre = keycloak.tokenParsed?.given_name || keycloak.tokenParsed?.preferred_username

  return (
    <div>
      <h1>Hola, {nombre}</h1>
      <Mensaje error={error} />

      <div className="tarjetas">
        <div className="tarjeta">
          <div className="numero">{bajoMinimo.length}</div>
          <div>articulos por debajo del stock minimo</div>
        </div>
        <div className="tarjeta">
          <div className="numero">{vencimientos.filter(v => !v.vencida).length}</div>
          <div>lotes que vencen en los proximos 30 dias</div>
        </div>
        <div className="tarjeta">
          <div className="numero">{vencimientos.filter(v => v.vencida).length}</div>
          <div>lotes vencidos con stock</div>
        </div>
      </div>

      <h2>Accesos rapidos</h2>
      <div className="acciones">
        {tieneRol('VENDEDOR', 'OPERARIO', 'ADMIN') && <Link className="boton" to="/venta">Registrar una venta</Link>}
        <Link className="boton" to="/stock">Consultar stock</Link>
        {tieneRol('OPERARIO', 'ADMIN') && <Link className="boton" to="/movimientos">Ingresar mercaderia</Link>}
        {tieneRol('OPERARIO', 'ADMIN') && <Link className="boton" to="/ordenes">Recibir una orden de compra</Link>}
        {tieneRol('COMPRAS', 'ADMIN') && <Link className="boton" to="/reposicion">Ver que hay que reponer</Link>}
      </div>

      {bajoMinimo.length > 0 && (
        <>
          <h2>Bajo el stock minimo</h2>
          <table>
            <thead><tr><th>SKU</th><th>Articulo</th><th>Disponible</th><th>Minimo</th></tr></thead>
            <tbody>
              {bajoMinimo.map(a => (
                <tr key={a.articuloId}>
                  <td>{a.sku}</td><td>{a.nombre}</td>
                  <td className="num">{cantidad(a.stockDisponible)}</td>
                  <td className="num">{cantidad(a.stockMinimo)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}

      {vencimientos.length > 0 && (
        <>
          <h2>Vencimientos</h2>
          <table>
            <thead><tr><th>Deposito</th><th>SKU</th><th>Lote</th><th>Vence</th><th>Cantidad</th><th></th></tr></thead>
            <tbody>
              {vencimientos.map((v, i) => (
                <tr key={i}>
                  <td>{v.depositoCodigo}</td><td>{v.sku}</td><td>{v.lote}</td><td>{fecha(v.fechaVencimiento)}</td>
                  <td className="num">{cantidad(v.cantidad)}</td>
                  <td>{v.vencida ? <span className="etiqueta roja">VENCIDO</span> : `${v.diasParaVencer} dias`}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  )
}
