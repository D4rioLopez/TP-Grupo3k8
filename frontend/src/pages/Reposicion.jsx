import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, textoError } from '../api'
import Mensaje from '../components/Mensaje'
import SelectorDeposito from '../components/SelectorDeposito'
import { cantidad, fecha, pesos } from '../util'

export default function Reposicion() {
  const navegar = useNavigate()
  const [depositoId, setDepositoId] = useState(null)
  const [diasCobertura, setDiasCobertura] = useState(15)
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(false)

  const calcular = async (e) => {
    e.preventDefault()
    setError(null)
    setCargando(true)
    try {
      setResultado(await api(`/reposicion/sugerencias?depositoId=${depositoId}&diasCobertura=${diasCobertura}`))
    } catch (err) {
      setError(textoError(err))
    }
    setCargando(false)
  }

  const generarBorradores = async () => {
    setError(null)
    try {
      await api(`/reposicion/ordenes-borrador?depositoId=${depositoId}&diasCobertura=${diasCobertura}`, { method: 'POST' })
      navegar('/ordenes')
    } catch (err) {
      setError(textoError(err))
    }
  }

  const conProveedor = resultado?.sugerencias.filter(s => s.proveedorId) || []

  return (
    <div>
      <h1>Reposicion sugerida</h1>
      <p>Combina el stock y el consumo del deposito, el stock minimo, lo que ya esta pedido y el plazo de cada
        proveedor en dias habiles (descontando los feriados del pais del deposito).</p>
      <form className="filtros" onSubmit={calcular}>
        <SelectorDeposito valor={depositoId} onChange={setDepositoId} />
        <label>Dias de cobertura
          <input type="number" min="1" max="120" value={diasCobertura} onChange={e => setDiasCobertura(e.target.value)} />
        </label>
        <button type="submit" disabled={!depositoId || cargando}>{cargando ? 'Calculando...' : 'Calcular'}</button>
      </form>
      <Mensaje error={error} advertencias={resultado?.advertencias} />

      {resultado && (
        <>
          {!resultado.calendarioConfiable &&
            <p className="mensaje aviso">No se pudieron obtener los feriados: los plazos solo descuentan fines de semana.</p>}
          {resultado.sugerencias.length === 0
            ? <p className="mensaje ok">No hay nada para reponer en {resultado.depositoCodigo}.</p>
            : (
              <>
                <table>
                  <thead>
                    <tr><th>SKU</th><th>Articulo</th><th>Disponible</th><th>En camino</th><th>Consumo/dia</th><th>Minimo</th>
                      <th>Proveedor</th><th>Llega</th><th>Punto de pedido</th><th>Pedir</th><th>Costo</th></tr>
                  </thead>
                  <tbody>
                    {resultado.sugerencias.map(s => (
                      <tr key={s.articuloId} title={s.explicacion}>
                        <td>{s.sku}</td><td>{s.nombre}<br /><small>{s.explicacion}</small></td>
                        <td className="num">{cantidad(s.stockDisponible)}</td>
                        <td className="num">{cantidad(s.pendienteEnOrdenes)}</td>
                        <td className="num">{cantidad(s.consumoDiarioPromedio)}</td>
                        <td className="num">{cantidad(s.stockMinimo)}</td>
                        <td>{s.proveedorRazonSocial || <span className="etiqueta roja">sin proveedor</span>}</td>
                        <td>{fecha(s.fechaEstimadaEntrega)}</td>
                        <td className="num">{cantidad(s.puntoDePedido)}</td>
                        <td className="num"><b>{cantidad(s.cantidadSugerida)}</b></td>
                        <td className="num">{s.precioUnitario ? pesos(s.precioUnitario * s.cantidadSugerida) : '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {conProveedor.length > 0 &&
                  <button onClick={generarBorradores}>Generar ordenes en borrador ({new Set(conProveedor.map(s => s.proveedorId)).size} proveedores)</button>}
              </>
            )}
        </>
      )}
    </div>
  )
}
