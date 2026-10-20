import { useEffect, useState } from 'react'
import { api, textoError } from '../api'
import { tieneRol } from '../keycloak'
import Mensaje from '../components/Mensaje'
import SelectorDeposito from '../components/SelectorDeposito'
import { cantidad, fecha } from '../util'

const MOTIVOS_INGRESO = ['STOCK_INICIAL', 'DEVOLUCION_CLIENTE', 'COMPRA']
const MOTIVOS_EGRESO = ['CONSUMO_INTERNO', 'ROTURA', 'VENCIMIENTO', 'DEVOLUCION_PROVEEDOR', 'VENTA']

export default function Movimientos() {
  const [pestania, setPestania] = useState('ingreso')
  const [articulos, setArticulos] = useState([])

  // lista de articulos para los combos (con sus reglas de lote/vencimiento)
  useEffect(() => {
    api('/articulos?activo=true&tamanio=100').then(r => setArticulos(r.contenido)).catch(() => setArticulos([]))
  }, [])

  const pestanias = [
    ['ingreso', 'Ingreso'], ['egreso', 'Egreso'], ['transferencia', 'Transferencia'],
    ...(tieneRol('ADMIN') ? [['ajuste', 'Ajuste (inventario fisico)']] : []),
    ['historial', 'Historial']
  ]

  return (
    <div>
      <h1>Movimientos de stock</h1>
      <div className="pestanias">
        {pestanias.map(([clave, texto]) => (
          <button key={clave} className={pestania === clave ? 'activa' : ''} onClick={() => setPestania(clave)}>{texto}</button>
        ))}
      </div>
      {pestania === 'ingreso' && <Operacion tipo="ingreso" articulos={articulos} />}
      {pestania === 'egreso' && <Operacion tipo="egreso" articulos={articulos} />}
      {pestania === 'transferencia' && <Operacion tipo="transferencia" articulos={articulos} />}
      {pestania === 'ajuste' && <Ajuste articulos={articulos} />}
      {pestania === 'historial' && <Historial />}
    </div>
  )
}

const itemVacio = () => ({ articuloId: '', cantidad: '', lote: '', fechaVencimiento: '' })

// Formulario comun para ingreso, egreso y transferencia
function Operacion({ tipo, articulos }) {
  const [depositoId, setDepositoId] = useState(null)
  const [destinoId, setDestinoId] = useState(null)
  const [motivo, setMotivo] = useState(tipo === 'ingreso' ? 'STOCK_INICIAL' : 'CONSUMO_INTERNO')
  const [referencia, setReferencia] = useState('')
  const [observacion, setObservacion] = useState('')
  const [items, setItems] = useState([itemVacio()])
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)

  const esIngreso = tipo === 'ingreso'
  const buscarArticulo = (id) => articulos.find(a => a.id === Number(id))
  const cambiarItem = (i, campo, valor) => setItems(items.map((it, j) => j === i ? { ...it, [campo]: valor } : it))

  const enviar = async (e) => {
    e.preventDefault()
    setError(null)
    setResultado(null)
    const itemsBody = items.map(it => ({
      articuloId: Number(it.articuloId),
      cantidad: Number(it.cantidad),
      lote: it.lote || null,
      ...(esIngreso ? { fechaVencimiento: it.fechaVencimiento || null } : {})
    }))
    try {
      let r
      if (tipo === 'transferencia') {
        r = await api('/movimientos/transferencias', {
          method: 'POST',
          body: { depositoOrigenId: depositoId, depositoDestinoId: destinoId, referencia: referencia || null, observacion: observacion || null, items: itemsBody }
        })
      } else {
        r = await api(`/movimientos/${esIngreso ? 'ingresos' : 'egresos'}`, {
          method: 'POST',
          body: { depositoId, motivo, referencia: referencia || null, observacion: observacion || null, items: itemsBody }
        })
      }
      setResultado(r)
      setItems([itemVacio()])
      setReferencia('')
    } catch (err) {
      setError(textoError(err))
    }
  }

  return (
    <form className="panel" onSubmit={enviar}>
      <div className="grilla">
        <SelectorDeposito valor={depositoId} onChange={setDepositoId} etiqueta={tipo === 'transferencia' ? 'Deposito origen' : 'Deposito'} />
        {tipo === 'transferencia'
          ? <SelectorDeposito valor={destinoId} onChange={setDestinoId} etiqueta="Deposito destino" />
          : <label>Motivo
              <select value={motivo} onChange={e => setMotivo(e.target.value)}>
                {(esIngreso ? MOTIVOS_INGRESO : MOTIVOS_EGRESO).map(m => <option key={m}>{m}</option>)}
              </select>
            </label>}
        <label>Referencia (remito, ticket...){motivo === 'COMPRA' && esIngreso ? ' *' : ''}
          <input value={referencia} onChange={e => setReferencia(e.target.value)} placeholder="opcional: se genera sola" />
        </label>
        <label>Observacion<input value={observacion} onChange={e => setObservacion(e.target.value)} /></label>
      </div>

      <table>
        <thead><tr><th>Articulo</th><th>Cantidad</th><th>Lote</th>{esIngreso && <th>Vencimiento</th>}<th></th></tr></thead>
        <tbody>
          {items.map((it, i) => {
            const art = buscarArticulo(it.articuloId)
            return (
              <tr key={i}>
                <td>
                  <select value={it.articuloId} onChange={e => cambiarItem(i, 'articuloId', e.target.value)} required>
                    <option value="">Elegir...</option>
                    {articulos.map(a => <option key={a.id} value={a.id}>{a.sku} - {a.nombre}</option>)}
                  </select>
                </td>
                <td>
                  <input type="number" step="any" min="0" value={it.cantidad} onChange={e => cambiarItem(i, 'cantidad', e.target.value)} required />
                  {art && <small> {art.unidadMedida.toLowerCase()}</small>}
                </td>
                <td>
                  {art?.controlaLote
                    ? <input value={it.lote} onChange={e => cambiarItem(i, 'lote', e.target.value)}
                        required={esIngreso} placeholder={esIngreso ? 'obligatorio' : 'vacio = FEFO'} />
                    : <small>no usa lote</small>}
                </td>
                {esIngreso && (
                  <td>
                    {art?.controlaVencimiento
                      ? <input type="date" value={it.fechaVencimiento} onChange={e => cambiarItem(i, 'fechaVencimiento', e.target.value)} required />
                      : <small>no vence</small>}
                  </td>
                )}
                <td>{items.length > 1 && <button type="button" className="chico peligro" onClick={() => setItems(items.filter((_, j) => j !== i))}>Quitar</button>}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
      <div className="acciones">
        <button type="button" className="secundario" onClick={() => setItems([...items, itemVacio()])}>Agregar item</button>
        <button type="submit">Registrar {tipo}</button>
      </div>

      <Mensaje error={error} />
      {resultado && <ResultadoOperacion r={resultado} />}
    </form>
  )
}

function Ajuste({ articulos }) {
  const [depositoId, setDepositoId] = useState(null)
  const [form, setForm] = useState({ articuloId: '', lote: '', fechaVencimiento: '', cantidadContada: '', observacion: '' })
  const [error, setError] = useState(null)
  const [resultado, setResultado] = useState(null)
  const art = articulos.find(a => a.id === Number(form.articuloId))
  const cambiar = (c, v) => setForm({ ...form, [c]: v })

  const enviar = async (e) => {
    e.preventDefault()
    setError(null)
    setResultado(null)
    try {
      setResultado(await api('/movimientos/ajustes', {
        method: 'POST',
        body: {
          depositoId, articuloId: Number(form.articuloId), lote: form.lote || null,
          fechaVencimiento: form.fechaVencimiento || null, cantidadContada: Number(form.cantidadContada), observacion: form.observacion
        }
      }))
    } catch (err) {
      setError(textoError(err))
    }
  }

  return (
    <form className="panel" onSubmit={enviar}>
      <p>Se informa la cantidad <b>contada</b>; el sistema registra la diferencia con lo que tenia.</p>
      <div className="grilla">
        <SelectorDeposito valor={depositoId} onChange={setDepositoId} />
        <label>Articulo
          <select value={form.articuloId} onChange={e => cambiar('articuloId', e.target.value)} required>
            <option value="">Elegir...</option>
            {articulos.map(a => <option key={a.id} value={a.id}>{a.sku} - {a.nombre}</option>)}
          </select>
        </label>
        {art?.controlaLote && <label>Lote<input value={form.lote} onChange={e => cambiar('lote', e.target.value)} required /></label>}
        {art?.controlaVencimiento && <label>Vencimiento (si el lote es nuevo)
          <input type="date" value={form.fechaVencimiento} onChange={e => cambiar('fechaVencimiento', e.target.value)} /></label>}
        <label>Cantidad contada<input type="number" step="any" min="0" value={form.cantidadContada} onChange={e => cambiar('cantidadContada', e.target.value)} required /></label>
        <label>Motivo del ajuste<input value={form.observacion} onChange={e => cambiar('observacion', e.target.value)} required /></label>
      </div>
      <button type="submit">Registrar ajuste</button>
      <Mensaje error={error} />
      {resultado && <ResultadoOperacion r={resultado} />}
    </form>
  )
}

function ResultadoOperacion({ r }) {
  return (
    <div>
      <Mensaje ok={`Operacion ${r.referencia} registrada${r.repetida ? ' (ya existia: no se volvio a aplicar)' : ''}`}
        advertencias={r.advertencias} />
      <table>
        <thead><tr><th>Tipo</th><th>Deposito</th><th>SKU</th><th>Lote</th><th>Cantidad</th><th>Saldo del lote</th></tr></thead>
        <tbody>
          {r.movimientos.map(m => (
            <tr key={m.id}>
              <td>{m.tipo}</td><td>{m.depositoCodigo}</td><td>{m.sku}</td><td>{m.lote}</td>
              <td className="num">{cantidad(m.cantidad)}</td><td className="num">{cantidad(m.saldoLote)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function Historial() {
  const [depositoId, setDepositoId] = useState(null)
  const [pagina, setPagina] = useState(0)
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!depositoId) return
    api(`/movimientos?depositoId=${depositoId}&pagina=${pagina}&tamanio=20`).then(setDatos).catch(e => setError(textoError(e)))
  }, [depositoId, pagina])

  return (
    <div className="panel">
      <div className="filtros"><SelectorDeposito valor={depositoId} onChange={d => { setDepositoId(d); setPagina(0) }} /></div>
      <Mensaje error={error} />
      {datos && (
        <>
          <table>
            <thead><tr><th>Fecha</th><th>Tipo</th><th>Motivo</th><th>SKU</th><th>Lote</th><th>Cantidad</th><th>Saldo</th><th>Usuario</th><th>Referencia</th></tr></thead>
            <tbody>
              {datos.contenido.map(m => (
                <tr key={m.id}>
                  <td>{fecha(m.fecha)}</td><td>{m.tipo}</td><td>{m.motivo}</td><td>{m.sku}</td><td>{m.lote}</td>
                  <td className="num">{cantidad(m.cantidad)}</td><td className="num">{cantidad(m.saldoLote)}</td>
                  <td>{m.usuario}</td><td><small>{m.referencia}</small></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="paginado">
            <button className="secundario" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</button>
            <span>Pagina {pagina + 1} de {Math.max(datos.totalPaginas, 1)}</span>
            <button className="secundario" disabled={pagina + 1 >= datos.totalPaginas} onClick={() => setPagina(pagina + 1)}>Siguiente</button>
          </div>
        </>
      )}
    </div>
  )
}
