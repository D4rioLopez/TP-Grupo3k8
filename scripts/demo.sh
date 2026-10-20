#!/usr/bin/env bash
# =====================================================================================
# DepositoClick - demo / prueba de punta a punta (todo a traves del API Gateway)
#
# Requisitos: la solucion levantada (docker compose up -d --build) y curl.
# Uso:        bash scripts/demo.sh
#
# Recorre los procesos principales con los 4 roles y verifica el codigo HTTP de cada paso.
# Al final informa cuantos pasos dieron el resultado esperado.
# =====================================================================================

GW=${GW:-http://localhost:8080}
KC=${KC:-http://localhost:9090/realms/depositoclick/protocol/openid-connect/token}

OK=0
FALLAS=0
BODY=""

token() {
  curl -s -X POST "$KC" -d grant_type=password -d client_id=depositoclick-client \
       -d username="$1" -d password="$2" | sed -E 's/.*"access_token":"([^"]+)".*/\1/'
}

# req METODO PATH TOKEN BODY CODIGO_ESPERADO DESCRIPCION
req() {
  local metodo=$1 path=$2 tk=$3 data=$4 esperado=$5 desc=$6
  local args=(-s -o /tmp/dc_body.json -w "%{http_code}" -X "$metodo" "$GW$path" -H "Content-Type: application/json")
  [ -n "$tk" ] && args+=(-H "Authorization: Bearer $tk")
  [ -n "$data" ] && args+=(-d "$data")
  local codigo
  codigo=$(curl "${args[@]}")
  BODY=$(cat /tmp/dc_body.json)
  if [ "$codigo" == "$esperado" ]; then
    OK=$((OK + 1)); echo "  [OK]    $codigo  $desc"
  else
    FALLAS=$((FALLAS + 1)); echo "  [FALLO] $codigo (se esperaba $esperado)  $desc"; echo "          $BODY" | cut -c1-400
  fi
}

campo() { echo "$BODY" | grep -o "\"$1\":[^,}]*" | head -1 | cut -d: -f2- | tr -d '"'; }

fecha() { date -d "+$1 days" +%F 2>/dev/null || date -v+"$1"d +%F; }

echo "== Tokens (Keycloak, grant password) =="
ADMIN=$(token admin1 admin123)
COMPRAS=$(token compras1 compras123)
OPERARIO=$(token operario1 operario123)
VENDEDOR=$(token vendedor1 vendedor123)
for t in ADMIN COMPRAS OPERARIO VENDEDOR; do
  valor=${!t}
  if [ ${#valor} -gt 100 ]; then echo "  [OK]    token $t"; OK=$((OK+1)); else echo "  [FALLO] token $t"; FALLAS=$((FALLAS+1)); fi
done

echo; echo "== Seguridad en el gateway =="
req GET "/api/v1/articulos" "" "" 401 "sin token -> 401"
req GET "/api/v1/articulos" "invalido.token.x" "" 401 "token adulterado -> 401"
req GET "/api/v1/reposicion/sugerencias?depositoId=1" "$VENDEDOR" "" 403 "VENDEDOR no ve reposicion -> 403"
req POST "/api/v1/rubros" "$OPERARIO" '{"nombre":"Juguetes"}' 403 "OPERARIO no modifica el catalogo -> 403"

echo; echo "== Catalogo polimorfico =="
req GET "/api/v1/articulos?tamanio=50" "$VENDEDOR" "" 200 "VENDEDOR consulta el catalogo"
req GET "/api/v1/articulos/codigo-barras/7790001000019" "$VENDEDOR" "" 200 "PoS escanea un codigo de barras"
req POST "/api/v1/articulos" "$ADMIN" '{"sku":"IND-003","nombre":"Remera roja S","categoriaId":3,"unidadMedida":"UNIDAD","stockMinimo":5,"atributos":{"talle":"XXL","color":"Rojo"}}' 422 "talle que la categoria no admite -> 422"
req POST "/api/v1/articulos" "$ADMIN" '{"sku":"IND-003","codigoBarras":"7790001000014","nombre":"Remera roja S","categoriaId":3,"unidadMedida":"UNIDAD","stockMinimo":5,"atributos":{"talle":"S","color":"Rojo"}}' 422 "codigo de barras con verificador incorrecto -> 422"
req POST "/api/v1/articulos" "$ADMIN" '{"sku":"IND-003","nombre":"Remera roja S","categoriaId":3,"unidadMedida":"UNIDAD","stockMinimo":5,"atributos":{"talle":"S","color":"Rojo"}}' 201 "ADMIN crea una remera (atributos validos)"
req POST "/api/v1/articulos" "$ADMIN" '{"sku":"IND-003","nombre":"x","categoriaId":3,"unidadMedida":"UNIDAD","stockMinimo":5,"atributos":{"talle":"S","color":"Rojo"}}' 409 "SKU repetido -> 409"

echo; echo "== Stock inicial (ms-inventario valida cada articulo contra ms-catalogo) =="
V20=$(fecha 20); V45=$(fecha 45); V90=$(fecha 90); V200=$(fecha 200)
req POST "/api/v1/movimientos/ingresos" "$OPERARIO" "{\"depositoId\":1,\"motivo\":\"STOCK_INICIAL\",\"referencia\":\"INICIAL-CBA-001\",\"items\":[
  {\"articuloId\":1,\"cantidad\":30,\"lote\":\"L-LECHE-A\",\"fechaVencimiento\":\"$V45\"},
  {\"articuloId\":1,\"cantidad\":10,\"lote\":\"L-LECHE-B\",\"fechaVencimiento\":\"$V20\"},
  {\"articuloId\":2,\"cantidad\":15,\"lote\":\"L-YOG-1\",\"fechaVencimiento\":\"$V20\"},
  {\"articuloId\":3,\"cantidad\":40,\"lote\":\"L-YER-1\",\"fechaVencimiento\":\"$V200\"},
  {\"articuloId\":4,\"cantidad\":12},{\"articuloId\":5,\"cantidad\":3},
  {\"articuloId\":6,\"cantidad\":7.5},
  {\"articuloId\":7,\"cantidad\":250,\"lote\":\"BOB-0042\"},
  {\"articuloId\":8,\"cantidad\":50,\"lote\":\"IBU-77\",\"fechaVencimiento\":\"$V90\"}]}" 201 "ingreso de stock inicial en DEP-CBA"
req POST "/api/v1/movimientos/ingresos" "$OPERARIO" "{\"depositoId\":1,\"motivo\":\"STOCK_INICIAL\",\"referencia\":\"INICIAL-CBA-001\",\"items\":[{\"articuloId\":1,\"cantidad\":30,\"lote\":\"L-LECHE-A\",\"fechaVencimiento\":\"$V45\"}]}" 200 "misma referencia otra vez -> 200 (idempotente, no duplica)"
req POST "/api/v1/movimientos/ingresos" "$OPERARIO" '{"depositoId":1,"motivo":"STOCK_INICIAL","items":[{"articuloId":1,"cantidad":5}]}' 422 "leche sin lote ni vencimiento -> 422"
req POST "/api/v1/movimientos/ingresos" "$OPERARIO" '{"depositoId":1,"motivo":"STOCK_INICIAL","items":[{"articuloId":4,"cantidad":1.5}]}' 422 "1.5 remeras (UNIDAD) -> 422"

echo; echo "== Punto de venta (VENDEDOR) =="
req POST "/api/v1/movimientos/egresos" "$VENDEDOR" '{"depositoId":1,"motivo":"VENTA","referencia":"TICKET-0001-0001","items":[{"articuloId":1,"cantidad":25},{"articuloId":7,"cantidad":12.5}]}' 201 "venta de 25 leches (FEFO) y 12.5 m de cable"
echo "          primer lote despachado: $(campo lote)  (tiene que ser L-LECHE-B, el que vence antes)"
req POST "/api/v1/movimientos/egresos" "$VENDEDOR" '{"depositoId":1,"motivo":"VENTA","items":[{"articuloId":5,"cantidad":10}]}' 422 "vender mas de lo que hay -> 422"
req POST "/api/v1/movimientos/egresos" "$VENDEDOR" '{"depositoId":1,"motivo":"ROTURA","items":[{"articuloId":4,"cantidad":1}]}' 403 "VENDEDOR registrando una ROTURA -> 403"
req GET "/api/v1/stock/articulos/1" "$VENDEDOR" "" 200 "stock consolidado de la leche"
echo "          disponible: $(campo stockDisponible) | bajo minimo: $(campo bajoMinimo)"
req POST "/api/v1/movimientos/transferencias" "$OPERARIO" '{"depositoOrigenId":1,"depositoDestinoId":2,"items":[{"articuloId":3,"cantidad":10}]}' 201 "transferencia de yerba CBA -> MVD"
req POST "/api/v1/movimientos/ajustes" "$ADMIN" '{"depositoId":1,"articuloId":4,"cantidadContada":11,"observacion":"Conteo mensual"}' 201 "ajuste por inventario fisico (ADMIN)"
req GET "/api/v1/stock/bajo-minimo?depositoId=1" "$COMPRAS" "" 200 "articulos bajo el stock minimo"

echo; echo "== Calendario laboral (API publica externa de feriados) =="
req GET "/api/v1/calendario/feriados?pais=AR&anio=$(date +%Y)" "$VENDEDOR" "" 200 "feriados de Argentina $(date +%Y)"
echo "          fuente: $(campo fuente) | confiable: $(campo confiable)"
req GET "/api/v1/calendario/fecha-entrega?pais=UY&diasHabiles=5" "$COMPRAS" "" 200 "hoy + 5 dias habiles en Uruguay"
echo "          entrega: $(campo fechaEntrega) | dias corridos: $(campo diasCorridos)"

echo; echo "== Compras: orden de compra -> emision -> recepcion (colaboracion entre los 3 servicios) =="
req POST "/api/v1/ordenes-compra" "$COMPRAS" '{"proveedorId":1,"depositoDestinoId":1,"items":[{"articuloId":1,"cantidad":48},{"articuloId":2,"cantidad":24}]}' 201 "crear orden en BORRADOR"
OC=$(campo id)
req POST "/api/v1/ordenes-compra" "$COMPRAS" '{"proveedorId":1,"depositoDestinoId":1,"items":[{"articuloId":6,"cantidad":3}]}' 422 "articulo que el proveedor no vende -> 422"
req POST "/api/v1/ordenes-compra/$OC/emision" "$COMPRAS" "" 200 "emitir la orden $OC (calcula entrega con feriados)"
echo "          entrega estimada: $(campo fechaEntregaEstimada) | fuente: $(campo fuenteCalendario)"
req POST "/api/v1/ordenes-compra/$OC/emision" "$COMPRAS" "" 409 "emitirla dos veces -> 409"
req POST "/api/v1/ordenes-compra/$OC/recepciones" "$OPERARIO" "{\"observacion\":\"Remito 0001-123\",\"items\":[{\"articuloId\":1,\"cantidad\":20,\"lote\":\"L-LECHE-C\",\"fechaVencimiento\":\"$V45\"}]}" 201 "recepcion parcial (ingresa stock en inventario)"
echo "          estado: $(campo estado)"
req POST "/api/v1/ordenes-compra/$OC/recepciones" "$OPERARIO" '{"items":[{"articuloId":2,"cantidad":24}]}' 422 "yogur sin lote -> 422 (lo rechaza inventario)"
req POST "/api/v1/ordenes-compra/$OC/recepciones" "$OPERARIO" "{\"items\":[{\"articuloId\":1,\"cantidad\":29,\"lote\":\"L-LECHE-C\",\"fechaVencimiento\":\"$V45\"}]}" 422 "recibir mas de lo pendiente -> 422"
req POST "/api/v1/ordenes-compra/$OC/recepciones" "$OPERARIO" "{\"items\":[{\"articuloId\":1,\"cantidad\":28,\"lote\":\"L-LECHE-C\",\"fechaVencimiento\":\"$V45\"},{\"articuloId\":2,\"cantidad\":24,\"lote\":\"L-YOG-2\",\"fechaVencimiento\":\"$V20\"}]}" 201 "recepcion final"
echo "          estado: $(campo estado)"
req GET "/api/v1/movimientos/operaciones/OC-$OC-REC-1" "$OPERARIO" "" 200 "el ingreso quedo registrado en inventario con referencia OC-$OC-REC-1"

echo; echo "== Reposicion (inventario + catalogo + compras + feriados) =="
req POST "/api/v1/movimientos/egresos" "$VENDEDOR" '{"depositoId":1,"motivo":"VENTA","items":[{"articuloId":3,"cantidad":25},{"articuloId":4,"cantidad":9}]}' 201 "mas ventas (generan consumo)"
req GET "/api/v1/reposicion/sugerencias?depositoId=1&diasCobertura=15" "$COMPRAS" "" 200 "sugerencias de reposicion de DEP-CBA"
echo "          $(echo "$BODY" | grep -o '"sku":"[^"]*"\|"cantidadSugerida":[^,]*' | paste - - | tr '\n' ' ')"
req POST "/api/v1/reposicion/ordenes-borrador?depositoId=1&diasCobertura=15" "$COMPRAS" "" 201 "generar ordenes BORRADOR por proveedor"

echo; echo "== Resultado: $OK OK, $FALLAS fallas =="
[ $FALLAS -eq 0 ]
