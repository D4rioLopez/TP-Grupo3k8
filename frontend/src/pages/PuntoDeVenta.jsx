import { useRef, useState } from 'react'
import { api, textoError } from '../api'
import Mensaje from '../components/Mensaje'
import SelectorDeposito from '../components/SelectorDeposito'
import { cantidad, fecha, permiteDecimales } from '../util'

export default function PuntoDeVenta() {
  const [depositoId, setDepositoId] = useState(null)
  const [codigo, setCodigo] = useState('')
  const [carrito, setCarrito] = useState([])
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const inputRef = useRef(null)

  // Busca por codigo de barras (si son solo numeros) o por SKU, y consulta el stock disponible
  const agregar = async (e) => {
    e.preventDefault()
    const valor = codigo.trim()
    if (!valor) return
    setError(null)
    setResultado(null)
    try {
      let articulo
      if (/^\d{8,14}$/.test(valor)) {
        articulo = await api(`/articulos/codigo-barras/${valor}`)
      } else {
        articulo = await api(`/articulos/sku/${encodeURIComponent(valor)}`)
      }
      const stock = await api(`/stock/articulos/${articulo.id}?depositoId=${depositoId}`)
      const yaEsta = carrito.find(c => c.articulo.id === articulo.id)
      if (yaEsta) {
        setCarrito(carrito.map(c => c.articulo.id === articulo.id ? { ...c, cantidad: Number(c.cantidad) + 1 } : c))
      } else {
        setCarrito([...carrito, { articulo, disponible: stock.stockDisponible, cantidad: 1 }])
      }
      setCodigo('')
    } catch (err) {
      setError(err.status === 404 ? `No se encontro ningun articulo con "${valor}"` : textoError(err))
    }
    inputRef.current?.focus()
  }

  const cambiarCantidad = (id, valor) => {
    setCarrito(carrito.map(c => c.articulo.id === id ? { ...c, cantidad: valor } : c))
  }

  const quitar = (id) => setCarrito(carrito.filter(c => c.articulo.id !== id))

  const confirmar = async () => {
    setError(null)
    setEnviando(true)
    try {
      const r = await api('/movimientos/egresos', {
        method: 'POST',
        body: {
          depositoId,
          motivo: 'VENTA',
          // la referencia identifica el ticket: si se reenvia por error no se descuenta dos veces
          referencia: 'TICKET-' + Date.now(),
          items: carrito.map(c => ({ articuloId: c.articulo.id, cantidad: Number(c.cantidad) }))
        }
      })
      setResultado(r)
      setCarrito([])
    } catch (err) {
      setError(textoError(err))
    }
    setEnviando(false)
  }

  return (
    <div>
      <h1>Punto de venta</h1>
      <div className="filtros">
        <SelectorDeposito valor={depositoId} onChange={d => { setDepositoId(d); setCarrito([]) }} />
      </div>

      <form className="filtros" onSubmit={agregar}>
        <label className="ancho">
          Codigo de barras o SKU
          <input ref={inputRef} autoFocus value={codigo} onChange={e => setCodigo(e.target.value)}
            placeholder="Escanear o escribir (ej: 7790001000019, LAC-002)" />
        </label>
        <button type="submit" disabled={!depositoId}>Agregar</button>
      </form>
      <Mensaje error={error} />

      {carrito.length > 0 && (
        <>
          <table>
            <thead><tr><th>SKU</th><th>Articulo</th><th>Disponible</th><th>Cantidad</th><th></th></tr></thead>
            <tbody>
              {carrito.map(c => {
                const excede = Number(c.cantidad) > Number(c.disponible)
                return (
                  <tr key={c.articulo.id}>
                    <td>{c.articulo.sku}</td>
                    <td>{c.articulo.nombre}</td>
                    <td className="num">{cantidad(c.disponible)} {c.articulo.unidadMedida.toLowerCase()}</td>
                    <td>
                      <input type="number" className={excede ? 'campo-error' : ''} min="0"
                        step={permiteDecimales(c.articulo.unidadMedida) ? 'any' : '1'}
                        value={c.cantidad} onChange={e => cambiarCantidad(c.articulo.id, e.target.value)} />
                      {excede && <small className="texto-error"> supera el disponible</small>}
                    </td>
                    <td><button className="chico peligro" onClick={() => quitar(c.articulo.id)}>Quitar</button></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
          <button onClick={confirmar} disabled={enviando}>{enviando ? 'Registrando...' : 'Confirmar venta'}</button>
        </>
      )}

      {resultado && (
        <div className="panel">
          <h2>Venta registrada: {resultado.referencia}</h2>
          <Mensaje advertencias={resultado.advertencias} />
          <p>Lotes despachados (primero los que vencen antes):</p>
          <table>
            <thead><tr><th>SKU</th><th>Lote</th><th>Vence</th><th>Cantidad</th><th>Quedan en el lote</th></tr></thead>
            <tbody>
              {resultado.movimientos.map(m => (
                <tr key={m.id}>
                  <td>{m.sku}</td><td>{m.lote}</td><td>{fecha(m.fechaVencimiento)}</td>
                  <td className="num">{cantidad(m.cantidad)}</td><td className="num">{cantidad(m.saldoLote)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
