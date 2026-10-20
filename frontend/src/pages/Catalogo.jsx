import { useEffect, useState } from 'react'
import { api, textoError } from '../api'
import { tieneRol } from '../keycloak'
import Mensaje from '../components/Mensaje'
import { cantidad } from '../util'

const UNIDADES = ['UNIDAD', 'PAR', 'CAJA', 'KILOGRAMO', 'LITRO', 'METRO', 'METRO_CUADRADO']

export default function Catalogo() {
  const [texto, setTexto] = useState('')
  const [pagina, setPagina] = useState(0)
  const [resultado, setResultado] = useState(null)
  const [error, setError] = useState(null)
  const [ok, setOk] = useState(null)
  const [mostrarAlta, setMostrarAlta] = useState(false)
  const esAdmin = tieneRol('ADMIN')

  const buscar = (pag = 0) => {
    setError(null)
    setPagina(pag)
    api(`/articulos?pagina=${pag}&tamanio=15&texto=${encodeURIComponent(texto)}`)
      .then(setResultado)
      .catch(e => setError(textoError(e)))
  }

  useEffect(() => { buscar(0) }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const darDeBaja = async (a) => {
    if (!window.confirm(`Dar de baja ${a.sku}? No podra recibir stock ni comprarse.`)) return
    try {
      await api(`/articulos/${a.id}`, { method: 'DELETE' })
      setOk(`${a.sku} dado de baja`)
      buscar(pagina)
    } catch (e) {
      setError(textoError(e))
    }
  }

  const reactivar = async (a) => {
    try {
      await api(`/articulos/${a.id}/reactivacion`, { method: 'PATCH' })
      setOk(`${a.sku} reactivado`)
      buscar(pagina)
    } catch (e) {
      setError(textoError(e))
    }
  }

  return (
    <div>
      <h1>Catalogo</h1>
      <form className="filtros" onSubmit={e => { e.preventDefault(); buscar(0) }}>
        <label>
          Buscar por nombre o SKU
          <input value={texto} onChange={e => setTexto(e.target.value)} placeholder="ej: leche, FER-" />
        </label>
        <button type="submit">Buscar</button>
        {esAdmin && <button type="button" className="secundario" onClick={() => setMostrarAlta(!mostrarAlta)}>
          {mostrarAlta ? 'Cerrar alta' : 'Nuevo articulo'}</button>}
      </form>
      <Mensaje error={error} ok={ok} />

      {mostrarAlta && <AltaArticulo onCreado={a => { setOk(`Articulo ${a.sku} creado`); setMostrarAlta(false); buscar(0) }} />}

      {resultado && (
        <>
          <table>
            <thead>
              <tr><th>SKU</th><th>Nombre</th><th>Rubro / Categoria</th><th>Unidad</th><th>Minimo</th>
                <th>Atributos</th><th>Control</th><th>Estado</th>{esAdmin && <th></th>}</tr>
            </thead>
            <tbody>
              {resultado.contenido.map(a => (
                <tr key={a.id} className={a.activo ? '' : 'inactivo'}>
                  <td>{a.sku}<br /><small>{a.codigoBarras}</small></td>
                  <td>{a.nombre}</td>
                  <td>{a.rubroNombre} / {a.categoriaNombre}</td>
                  <td>{a.unidadMedida}</td>
                  <td className="num">{cantidad(a.stockMinimo)}</td>
                  <td><small>{Object.entries(a.atributos).map(([k, v]) => `${k}: ${v}`).join(' | ')}</small></td>
                  <td><small>{a.controlaLote ? 'lote ' : ''}{a.controlaVencimiento ? 'vencimiento' : ''}</small></td>
                  <td>{a.activo ? 'Activo' : 'De baja'}</td>
                  {esAdmin && <td>{a.activo
                    ? <button className="chico peligro" onClick={() => darDeBaja(a)}>Baja</button>
                    : <button className="chico" onClick={() => reactivar(a)}>Reactivar</button>}</td>}
                </tr>
              ))}
            </tbody>
          </table>
          <div className="paginado">
            <button className="secundario" disabled={pagina === 0} onClick={() => buscar(pagina - 1)}>Anterior</button>
            <span>Pagina {pagina + 1} de {Math.max(resultado.totalPaginas, 1)} ({resultado.totalElementos} articulos)</span>
            <button className="secundario" disabled={pagina + 1 >= resultado.totalPaginas} onClick={() => buscar(pagina + 1)}>Siguiente</button>
          </div>
        </>
      )}
    </div>
  )
}

// Formulario de alta: los campos de atributos se arman segun la categoria elegida
function AltaArticulo({ onCreado }) {
  const [categorias, setCategorias] = useState([])
  const [form, setForm] = useState({ sku: '', codigoBarras: '', nombre: '', categoriaId: '', unidadMedida: 'UNIDAD', stockMinimo: 0 })
  const [atributos, setAtributos] = useState({})
  const [error, setError] = useState(null)

  useEffect(() => {
    api('/categorias').then(setCategorias).catch(e => setError(textoError(e)))
  }, [])

  const categoria = categorias.find(c => c.id === Number(form.categoriaId))
  const cambiar = (campo, valor) => setForm({ ...form, [campo]: valor })

  const guardar = async (e) => {
    e.preventDefault()
    setError(null)
    try {
      const creado = await api('/articulos', {
        method: 'POST',
        body: {
          ...form,
          codigoBarras: form.codigoBarras || null,
          categoriaId: Number(form.categoriaId),
          stockMinimo: Number(form.stockMinimo),
          atributos
        }
      })
      onCreado(creado)
    } catch (err) {
      setError(textoError(err))
    }
  }

  return (
    <form className="panel" onSubmit={guardar}>
      <h2>Nuevo articulo</h2>
      <div className="grilla">
        <label>SKU<input value={form.sku} onChange={e => cambiar('sku', e.target.value)} required /></label>
        <label>Codigo de barras (opcional)<input value={form.codigoBarras} onChange={e => cambiar('codigoBarras', e.target.value)} /></label>
        <label>Nombre<input value={form.nombre} onChange={e => cambiar('nombre', e.target.value)} required /></label>
        <label>
          Categoria
          <select value={form.categoriaId} onChange={e => { cambiar('categoriaId', e.target.value); setAtributos({}) }} required>
            <option value="">Elegir...</option>
            {categorias.map(c => <option key={c.id} value={c.id}>{c.rubroNombre} / {c.nombre}</option>)}
          </select>
        </label>
        <label>
          Unidad de medida
          <select value={form.unidadMedida} onChange={e => cambiar('unidadMedida', e.target.value)}>
            {UNIDADES.map(u => <option key={u}>{u}</option>)}
          </select>
        </label>
        <label>Stock minimo<input type="number" step="any" min="0" value={form.stockMinimo} onChange={e => cambiar('stockMinimo', e.target.value)} /></label>
      </div>

      {categoria && (
        <>
          <h3>Atributos de "{categoria.nombre}"
            <small> {categoria.controlaLote && '· controla lote '}{categoria.controlaVencimiento && '· controla vencimiento'}</small></h3>
          <div className="grilla">
            {categoria.atributos.map(a => (
              <label key={a.nombre}>
                {a.nombre}{a.obligatorio ? ' *' : ''} <small>({a.tipo})</small>
                <CampoAtributo atributo={a} valor={atributos[a.nombre] || ''}
                  onChange={v => setAtributos({ ...atributos, [a.nombre]: v })} />
              </label>
            ))}
          </div>
        </>
      )}
      <Mensaje error={error} />
      <button type="submit">Guardar</button>
    </form>
  )
}

function CampoAtributo({ atributo, valor, onChange }) {
  if (atributo.tipo === 'OPCION') {
    return (
      <select value={valor} onChange={e => onChange(e.target.value)}>
        <option value="">-</option>
        {atributo.opciones.map(o => <option key={o}>{o}</option>)}
      </select>
    )
  }
  if (atributo.tipo === 'BOOLEANO') {
    return (
      <select value={valor} onChange={e => onChange(e.target.value)}>
        <option value="">-</option><option value="true">Si</option><option value="false">No</option>
      </select>
    )
  }
  const tipoInput = atributo.tipo === 'FECHA' ? 'date' : (atributo.tipo === 'NUMERO' || atributo.tipo === 'ENTERO') ? 'number' : 'text'
  return <input type={tipoInput} step="any" value={valor} onChange={e => onChange(e.target.value)} />
}
