import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, textoError } from '../api'
import Mensaje from '../components/Mensaje'
import SelectorDeposito from '../components/SelectorDeposito'
import { pesos } from '../util'

export default function NuevaOrden() {
  const navegar = useNavigate()
  const [proveedores, setProveedores] = useState([])
  const [proveedorId, setProveedorId] = useState('')
  const [articulosProveedor, setArticulosProveedor] = useState([])
  const [depositoId, setDepositoId] = useState(null)
  const [fechaSolicitada, setFechaSolicitada] = useState('')
  const [cantidades, setCantidades] = useState({})
  const [error, setError] = useState(null)

  useEffect(() => {
    api('/proveedores?activo=true').then(setProveedores).catch(e => setError(textoError(e)))
  }, [])

  // al elegir proveedor se muestran solo los articulos que vende (con su precio pactado)
  useEffect(() => {
    setCantidades({})
    if (!proveedorId) { setArticulosProveedor([]); return }
    api(`/proveedores/${proveedorId}/articulos`).then(setArticulosProveedor).catch(e => setError(textoError(e)))
  }, [proveedorId])

  const proveedor = proveedores.find(p => p.id === Number(proveedorId))

  const crear = async (e) => {
    e.preventDefault()
    setError(null)
    const items = Object.entries(cantidades)
      .filter(([, c]) => Number(c) > 0)
      .map(([articuloId, c]) => ({ articuloId: Number(articuloId), cantidad: Number(c) }))
    if (items.length === 0) {
      setError('Indica la cantidad de al menos un articulo')
      return
    }
    try {
      const orden = await api('/ordenes-compra', {
        method: 'POST',
        body: { proveedorId: Number(proveedorId), depositoDestinoId: depositoId, fechaEntregaSolicitada: fechaSolicitada || null, items }
      })
      navegar(`/ordenes/${orden.id}`)
    } catch (err) {
      setError(textoError(err))
    }
  }

  return (
    <form className="panel" onSubmit={crear}>
      <h1>Nueva orden de compra</h1>
      <div className="grilla">
        <label>Proveedor
          <select value={proveedorId} onChange={e => setProveedorId(e.target.value)} required>
            <option value="">Elegir...</option>
            {proveedores.map(p => <option key={p.id} value={p.id}>{p.razonSocial}</option>)}
          </select>
        </label>
        <SelectorDeposito valor={depositoId} onChange={setDepositoId} etiqueta="Deposito destino" />
        <label>Fecha de entrega solicitada (opcional)
          <input type="date" value={fechaSolicitada} onChange={e => setFechaSolicitada(e.target.value)} />
        </label>
      </div>
      {proveedor && <p>Plazo de entrega del proveedor: <b>{proveedor.leadTimeDiasHabiles} dias habiles</b> (la fecha se calcula al emitir, con los feriados del pais del deposito).</p>}

      {articulosProveedor.length > 0 && (
        <table>
          <thead><tr><th>SKU</th><th>Articulo</th><th>Precio pactado</th><th>Cantidad a pedir</th></tr></thead>
          <tbody>
            {articulosProveedor.map(a => (
              <tr key={a.articuloId}>
                <td>{a.sku}</td><td>{a.nombreArticulo}</td><td className="num">{pesos(a.precioUnitario)}</td>
                <td><input type="number" step="any" min="0" value={cantidades[a.articuloId] || ''}
                  onChange={e => setCantidades({ ...cantidades, [a.articuloId]: e.target.value })} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <Mensaje error={error} />
      <button type="submit" disabled={!proveedorId}>Crear en borrador</button>
    </form>
  )
}
