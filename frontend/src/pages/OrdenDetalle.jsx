import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, textoError } from '../api'
import { tieneRol } from '../keycloak'
import Mensaje from '../components/Mensaje'
import { cantidad, fecha, pesos } from '../util'

export default function OrdenDetalle() {
  const { id } = useParams()
  const [orden, setOrden] = useState(null)
  const [error, setError] = useState(null)
  const [ok, setOk] = useState(null)
  const [advertencias, setAdvertencias] = useState(null)

  const cargar = () => api(`/ordenes-compra/${id}`).then(setOrden).catch(e => setError(textoError(e)))
  useEffect(() => { cargar() }, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  const emitir = async () => {
    setError(null); setOk(null)
    try {
      const r = await api(`/ordenes-compra/${id}/emision`, { method: 'POST' })
      setOrden(r)
      setAdvertencias(r.advertencias)
      setOk('Orden emitida. Entrega estimada: ' + fecha(r.fechaEntregaEstimada)
        + (r.feriadosConsiderados?.length ? ' (feriados tenidos en cuenta: ' + r.feriadosConsiderados.join(', ') + ')' : ''))
    } catch (e) {
      setError(textoError(e))
    }
  }

  const cancelar = async () => {
    const motivo = window.prompt('Motivo de la cancelacion')
    if (!motivo) return
    setError(null); setOk(null)
    try {
      setOrden(await api(`/ordenes-compra/${id}/cancelacion`, { method: 'POST', body: { motivo } }))
      setOk('Orden cancelada')
    } catch (e) {
      setError(textoError(e))
    }
  }

  if (!orden) return <Mensaje error={error} />

  const puedeGestionar = tieneRol('COMPRAS', 'ADMIN')
  const puedeRecibir = tieneRol('OPERARIO', 'ADMIN') && (orden.estado === 'EMITIDA' || orden.estado === 'RECIBIDA_PARCIAL')

  return (
    <div>
      <p><Link to="/ordenes">← Volver a las ordenes</Link></p>
      <h1>Orden de compra N° {orden.id} <span className={'etiqueta ' + orden.estado.toLowerCase()}>{orden.estado}</span></h1>
      <div className="grilla datos">
        <div><b>Proveedor:</b> {orden.proveedorRazonSocial} ({orden.leadTimeDiasHabiles} dias habiles)</div>
        <div><b>Deposito destino:</b> {orden.depositoCodigo} ({orden.paisDeposito})</div>
        <div><b>Creada:</b> {fecha(orden.fechaCreacion)} por {orden.creadoPor}</div>
        <div><b>Emitida:</b> {fecha(orden.fechaEmision)}</div>
        <div><b>Entrega solicitada:</b> {fecha(orden.fechaEntregaSolicitada)}</div>
        <div><b>Entrega estimada:</b> {fecha(orden.fechaEntregaEstimada)}
          {orden.fuenteCalendario && <small> (feriados: {orden.fuenteCalendario}{orden.calendarioConfiable === false ? ', NO confiable' : ''})</small>}</div>
        {orden.motivoCancelacion && <div><b>Motivo de cancelacion:</b> {orden.motivoCancelacion}</div>}
      </div>

      <Mensaje error={error} ok={ok} advertencias={advertencias} />

      {puedeGestionar && (
        <div className="acciones">
          {orden.estado === 'BORRADOR' && <button onClick={emitir}>Emitir</button>}
          {(orden.estado === 'BORRADOR' || orden.estado === 'EMITIDA') &&
            <button className="peligro" onClick={cancelar}>Cancelar orden</button>}
        </div>
      )}

      <table>
        <thead><tr><th>SKU</th><th>Articulo</th><th>Unidad</th><th>Pedido</th><th>Recibido</th><th>Pendiente</th><th>Precio</th><th>Subtotal</th></tr></thead>
        <tbody>
          {orden.detalles.map(d => (
            <tr key={d.articuloId}>
              <td>{d.sku}</td><td>{d.nombreArticulo}</td><td>{d.unidadMedida}</td>
              <td className="num">{cantidad(d.cantidadPedida)}</td><td className="num">{cantidad(d.cantidadRecibida)}</td>
              <td className="num">{cantidad(d.pendiente)}</td><td className="num">{pesos(d.precioUnitario)}</td>
              <td className="num">{pesos(d.subtotal)}</td>
            </tr>
          ))}
          <tr><td colSpan="7"><b>Total</b></td><td className="num"><b>{pesos(orden.total)}</b></td></tr>
        </tbody>
      </table>

      {puedeRecibir && <Recepcion orden={orden} onRecibida={(r) => {
        setOrden(r); setAdvertencias(r.advertencias); setError(null)
        setOk('Recepcion registrada: el stock ya ingreso al deposito ' + r.depositoCodigo)
      }} />}

      {orden.recepciones.length > 0 && (
        <>
          <h2>Recepciones</h2>
          {orden.recepciones.map(r => (
            <div key={r.numero} className="panel">
              <b>Recepcion {r.numero}</b> — {fecha(r.fecha)} por {r.usuario} — referencia en inventario: <code>{r.referenciaInventario}</code>
              {r.observacion && <> — {r.observacion}</>}
              <ul>
                {r.items.map((i, k) => (
                  <li key={k}>Articulo {i.articuloId}: {cantidad(i.cantidad)}{i.lote ? ` (lote ${i.lote}` : ''}{i.fechaVencimiento ? `, vence ${fecha(i.fechaVencimiento)})` : (i.lote ? ')' : '')}</li>
                ))}
              </ul>
            </div>
          ))}
        </>
      )}
    </div>
  )
}

// Formulario para registrar lo que llego (puede ser una parte de lo pedido)
function Recepcion({ orden, onRecibida }) {
  const pendientes = orden.detalles.filter(d => Number(d.pendiente) > 0)
  const [reglas, setReglas] = useState({})
  const [filas, setFilas] = useState({})
  const [observacion, setObservacion] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  // pedimos al catalogo si cada articulo usa lote / vencimiento para mostrar esos campos
  useEffect(() => {
    const ids = pendientes.map(d => d.articuloId).join(',')
    if (!ids) return
    api(`/articulos/reglas-stock?ids=${ids}`)
      .then(lista => setReglas(Object.fromEntries(lista.map(r => [r.articuloId, r]))))
      .catch(() => setReglas({}))
    const inicial = {}
    pendientes.forEach(d => { inicial[d.articuloId] = { cantidad: d.pendiente, lote: '', fechaVencimiento: '' } })
    setFilas(inicial)
  }, [orden.id, orden.recepciones.length]) // eslint-disable-line react-hooks/exhaustive-deps

  const cambiar = (articuloId, campo, valor) => setFilas({ ...filas, [articuloId]: { ...filas[articuloId], [campo]: valor } })

  const recibir = async (e) => {
    e.preventDefault()
    setError(null)
    const items = Object.entries(filas)
      .filter(([, f]) => Number(f.cantidad) > 0)
      .map(([articuloId, f]) => ({
        articuloId: Number(articuloId), cantidad: Number(f.cantidad),
        lote: f.lote || null, fechaVencimiento: f.fechaVencimiento || null
      }))
    if (items.length === 0) {
      setError('No hay cantidades para recibir')
      return
    }
    setEnviando(true)
    try {
      onRecibida(await api(`/ordenes-compra/${orden.id}/recepciones`, {
        method: 'POST', body: { observacion: observacion || null, items }
      }))
    } catch (err) {
      setError(textoError(err))
    }
    setEnviando(false)
  }

  return (
    <form className="panel" onSubmit={recibir}>
      <h2>Registrar recepcion</h2>
      <p>Indica lo que llego realmente (puede ser menos de lo pendiente). Poner 0 en lo que no llego.</p>
      <table>
        <thead><tr><th>SKU</th><th>Pendiente</th><th>Cantidad recibida</th><th>Lote</th><th>Vencimiento</th></tr></thead>
        <tbody>
          {pendientes.map(d => {
            const r = reglas[d.articuloId]
            const f = filas[d.articuloId] || {}
            return (
              <tr key={d.articuloId}>
                <td>{d.sku}</td>
                <td className="num">{cantidad(d.pendiente)}</td>
                <td><input type="number" step="any" min="0" value={f.cantidad ?? ''} onChange={e => cambiar(d.articuloId, 'cantidad', e.target.value)} /></td>
                <td>{r?.controlaLote ? <input value={f.lote || ''} onChange={e => cambiar(d.articuloId, 'lote', e.target.value)} placeholder="obligatorio" /> : <small>no usa lote</small>}</td>
                <td>{r?.controlaVencimiento ? <input type="date" value={f.fechaVencimiento || ''} onChange={e => cambiar(d.articuloId, 'fechaVencimiento', e.target.value)} /> : <small>no vence</small>}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
      <label>Observacion (ej: numero de remito)<input value={observacion} onChange={e => setObservacion(e.target.value)} /></label>
      <Mensaje error={error} />
      <button type="submit" disabled={enviando}>{enviando ? 'Registrando...' : 'Registrar recepcion'}</button>
    </form>
  )
}
