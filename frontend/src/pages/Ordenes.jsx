import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, textoError } from '../api'
import { tieneRol } from '../keycloak'
import Mensaje from '../components/Mensaje'
import { fecha, pesos } from '../util'

const ESTADOS = ['', 'BORRADOR', 'EMITIDA', 'RECIBIDA_PARCIAL', 'RECIBIDA', 'CANCELADA']

export default function Ordenes() {
  const [estado, setEstado] = useState('')
  const [ordenes, setOrdenes] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    api('/ordenes-compra' + (estado ? `?estado=${estado}` : ''))
      .then(setOrdenes)
      .catch(e => setError(textoError(e)))
  }, [estado])

  return (
    <div>
      <h1>Ordenes de compra</h1>
      <div className="filtros">
        <label>Estado
          <select value={estado} onChange={e => setEstado(e.target.value)}>
            {ESTADOS.map(e => <option key={e} value={e}>{e || 'Todos'}</option>)}
          </select>
        </label>
        {tieneRol('COMPRAS', 'ADMIN') && <Link className="boton" to="/ordenes/nueva">Nueva orden</Link>}
      </div>
      <Mensaje error={error} />
      <table>
        <thead><tr><th>N°</th><th>Estado</th><th>Proveedor</th><th>Deposito</th><th>Creada</th><th>Entrega estimada</th><th>Total</th><th></th></tr></thead>
        <tbody>
          {ordenes.length === 0 && <tr><td colSpan="8">No hay ordenes</td></tr>}
          {ordenes.map(o => (
            <tr key={o.id}>
              <td>{o.id}</td>
              <td><span className={'etiqueta ' + o.estado.toLowerCase()}>{o.estado}</span></td>
              <td>{o.proveedorRazonSocial}</td>
              <td>{o.depositoCodigo}</td>
              <td>{fecha(o.fechaCreacion)}</td>
              <td>{fecha(o.fechaEntregaEstimada)}</td>
              <td className="num">{pesos(o.total)}</td>
              <td><Link to={`/ordenes/${o.id}`}>Ver</Link></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
