// Formatos para mostrar numeros y fechas

export function cantidad(valor) {
  if (valor === null || valor === undefined) return '-'
  return Number(valor).toLocaleString('es-AR', { maximumFractionDigits: 3 })
}

export function pesos(valor) {
  if (valor === null || valor === undefined) return '-'
  return Number(valor).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' })
}

export function fecha(valor) {
  if (!valor) return '-'
  // "2026-10-18" o "2026-10-18T20:21:27"
  const [f, h] = valor.split('T')
  const [a, m, d] = f.split('-')
  return h ? `${d}/${m}/${a} ${h.substring(0, 5)}` : `${d}/${m}/${a}`
}

// Las unidades UNIDAD, PAR y CAJA no aceptan decimales
export function permiteDecimales(unidad) {
  return !['UNIDAD', 'PAR', 'CAJA'].includes(unidad)
}
