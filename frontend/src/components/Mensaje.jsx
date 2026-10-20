// Muestra un error o un mensaje de exito. Si no hay nada que mostrar no renderiza nada.
export default function Mensaje({ error, ok, advertencias }) {
  return (
    <>
      {error && <p className="mensaje error">{error}</p>}
      {ok && <p className="mensaje ok">{ok}</p>}
      {advertencias?.map((a, i) => <p key={i} className="mensaje aviso">{a}</p>)}
    </>
  )
}
