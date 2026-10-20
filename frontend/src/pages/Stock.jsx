import { useEffect, useState } from 'react'
import { api, textoError } from '../api'
import Mensaje from '../components/Mensaje'
import SelectorDeposito from '../components/SelectorDeposito'
import { cantidad, fecha } from '../util'

export default function Stock() {
  const [depositoId, setDepositoId] = useState(null)
  const [vista, setVista] = useState('existencias')
  const [filas, setFilas] = useState([])
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!depositoId) return
    setError(null)
    const url = vista === 'existencias' ? `/stock?depositoId=${depositoId}` : `/stock/bajo-minimo?depositoId=${depositoId}`
    api(url).then(setFilas).catch(e => { setFilas([]); setError(textoError(e)) })
  }, [depositoId, vista])

  return (
    <div>
      <h1>Stock</h1>
      <div className="filtros">
        <SelectorDeposito valor={depositoId} onChange={setDepositoId} />
        <label>
          Ver
          <select value={vista} onChange={e => setVista(e.target.value)}>
            <option value="existencias">Existencias por lote</option>
            <option value="bajo-minimo">Bajo el stock minimo</option>
          </select>
        </label>
      </div>
      <Mensaje error={error} />

      {vista === 'existencias' ? (
        <table>
          <thead><tr><th>SKU</th><th>Lote</th><th>Vencimiento</th><th>Cantidad</th><th></th></tr></thead>
          <tbody>
            {filas.length === 0 && <tr><td colSpan="5">No hay stock en este deposito</td></tr>}
            {filas.map((e, i) => (
              <tr key={i}>
                <td>{e.sku}</td><td>{e.lote}</td><td>{fecha(e.fechaVencimiento)}</td>
                <td className="num">{cantidad(e.cantidad)}</td>
                <td>{e.vencida && <span className="etiqueta roja">VENCIDO</span>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <table>
          <thead><tr><th>SKU</th><th>Articulo</th><th>Unidad</th><th>Disponible</th><th>Vencido</th><th>Minimo</th></tr></thead>
          <tbody>
            {filas.length === 0 && <tr><td colSpan="6">Ningun articulo esta por debajo del minimo</td></tr>}
            {filas.map(a => (
              <tr key={a.articuloId}>
                <td>{a.sku}</td><td>{a.nombre}</td><td>{a.unidadMedida}</td>
                <td className="num">{cantidad(a.stockDisponible)}</td>
                <td className="num">{cantidad(a.stockVencido)}</td>
                <td className="num">{cantidad(a.stockMinimo)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
