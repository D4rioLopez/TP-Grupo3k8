import { useEffect, useState } from 'react'
import { api } from '../api'

// Combo de depositos activos. Si no hay uno elegido, selecciona el primero.
export default function SelectorDeposito({ valor, onChange, etiqueta = 'Deposito' }) {
  const [depositos, setDepositos] = useState([])

  useEffect(() => {
    api('/depositos?activo=true')
      .then(lista => {
        setDepositos(lista)
        if (!valor && lista.length > 0) onChange(lista[0].id)
      })
      .catch(() => setDepositos([]))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <label>
      {etiqueta}
      <select value={valor || ''} onChange={e => onChange(Number(e.target.value))}>
        {depositos.map(d => (
          <option key={d.id} value={d.id}>{d.codigo} - {d.nombre} ({d.pais})</option>
        ))}
      </select>
    </label>
  )
}
