#!/usr/bin/env bash
# Análise complementar (fora do avaliador congelado): sonda por HTTP comportamentos
# não cobertos pela suíte interna nem pelo avaliador 1.0.0 — barra final nas rotas
# e renovação do cookie CSRF após autenticação Basic.
# Uso: bash experiments/supplementary/probe-behaviors.sh <rótulo> <jar> <diretório-de-saída>
# Requer JAVA_HOME com JDK 17, Docker, curl e python.
set -eo pipefail
label=$1; jar=$2; out=$3
[ -n "$label" ] && [ -f "$jar" ] && [ -n "$out" ] || { echo 'uso: probe-behaviors.sh <rótulo> <jar> <saída>' >&2; exit 1; }
repo=$(cd "$(dirname "$0")/../.." && pwd)
image='postgres:16.10-bookworm@sha256:38471f330eb885e04de130b768d6db4e10469e2311879c7e5c699f6d2d8a1c74'
port=${PROBE_HTTP_PORT:-18090}
name="mes-probe-$(od -An -N6 -tx1 /dev/urandom | tr -d ' \n\r')"
mkdir -p "$out"; out=$(cd "$out" && pwd)
app_pid=''
cleanup() {
  [ -n "$app_pid" ] && kill "$app_pid" 2>/dev/null && wait "$app_pid" 2>/dev/null || true
  docker logs "$name" > "$out/postgres.log" 2>&1 || true
  docker rm -f -v "$name" > /dev/null 2>&1 || true
}
trap cleanup EXIT
schema=$(cygpath -w "$repo/src/main/resources/db/schema" 2>/dev/null || echo "$repo/src/main/resources/db/schema")
docker run -d --name "$name" -e POSTGRES_DB=shop_probe -e POSTGRES_USER=shop_probe \
  -e POSTGRES_PASSWORD=probe_local_only -p 127.0.0.1::5432 \
  -v "$schema:/docker-entrypoint-initdb.d:ro" "$image" > /dev/null
for i in $(seq 1 60); do
  docker exec "$name" pg_isready -U shop_probe -d shop_probe -h 127.0.0.1 > /dev/null 2>&1 && \
  docker exec "$name" psql -U shop_probe -d shop_probe -tAc "select to_regclass('shop.order_items')" 2>/dev/null | grep -q order_items && break
  sleep 1
done
for f in V001__products.sql V002__users.sql; do
  docker exec -i "$name" psql -q -v ON_ERROR_STOP=1 -U shop_probe -d shop_probe < "$repo/src/main/resources/db/demo/$f"
done
db_port=$(docker port "$name" 5432 | head -1 | sed 's/.*://' | tr -d '\r')
POSTGRES_HOST=127.0.0.1 POSTGRES_PORT="$db_port" POSTGRES_DB=shop_probe POSTGRES_USER=shop_probe \
POSTGRES_PASSWORD=probe_local_only SERVER_PORT="$port" "$JAVA_HOME/bin/java" -jar "$jar" > "$out/application.log" 2>&1 &
app_pid=$!
for i in $(seq 1 90); do
  curl -sf "http://127.0.0.1:$port/actuator/health" 2>/dev/null | grep -q '"UP"' && break
  kill -0 "$app_pid" 2>/dev/null || { echo 'aplicação encerrou' >&2; exit 1; }
  sleep 1
done
base="http://127.0.0.1:$port"
ADMIN='admin@local.test:Admin-local-123!'; CUST='cliente1@local.test:Cliente1-local-123!'
results="$out/results.tsv"; printf 'caso\tmétodo\trota\tstatus\tobservação\n' > "$results"
json() { python -I -c "import json,sys; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"; }
record() { printf '%s\t%s\t%s\t%s\t%s\n' "$1" "$2" "$3" "$4" "$5" >> "$results"; }
# token CSRF novo (cookie jar próprio) para escrita
csrf() { local jar=$1 cred=$2; curl -s -c "$jar" -b "$jar" -u "$cred" "$base/api/csrf" | json 'd["token"]'; }
call() { # caso método rota credencial [corpo]
  local jar="$out/jar-$1.txt" tok code
  if [ "$2" = GET ]; then
    code=$(curl -s -o "$out/body-$1.json" -w '%{http_code}' ${4:+-u "$4"} "$base$3")
  else
    tok=$(csrf "$jar" "$4")
    code=$(curl -s -o "$out/body-$1.json" -w '%{http_code}' -c "$jar" -b "$jar" -u "$4" -X "$2" \
      -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $tok" --data "$5" "$base$3")
  fi
  record "$1" "$2" "$3" "$code" ""; echo "$code"
}
pid=$(curl -s "$base/api/products?size=50" | json '[p["id"] for p in d["content"] if p["stockQuantity"]>=5][0]')
period='start=2020-01-01T00:00:00Z&end=2100-01-01T00:00:00Z'
sku="PROBE-$(date +%s)"
# Barra final: cada par (sem barra / com barra)
call S01a GET "/api/products" ""; call S01b GET "/api/products/" ""
call S02a GET "/api/products/$pid" ""; call S02b GET "/api/products/$pid/" ""
call S03a GET "/api/me" "$ADMIN"; call S03b GET "/api/me/" "$ADMIN"
call S04a GET "/api/csrf" "$ADMIN"; call S04b GET "/api/csrf/" "$ADMIN"
call S05a GET "/api/orders" "$CUST"; call S05b GET "/api/orders/" "$CUST"
call S06a GET "/api/admin/orders" "$ADMIN"; call S06b GET "/api/admin/orders/" "$ADMIN"
call S07a GET "/api/admin/reports/orders?$period" "$ADMIN"; call S07b GET "/api/admin/reports/orders/?$period" "$ADMIN"
call S08a POST "/api/admin/products" "$ADMIN" "{\"sku\":\"$sku-A\",\"name\":\"Sonda\",\"price\":10.00,\"stockQuantity\":5,\"active\":true}" > /dev/null
call S08b POST "/api/admin/products/" "$ADMIN" "{\"sku\":\"$sku-B\",\"name\":\"Sonda\",\"price\":10.00,\"stockQuantity\":5,\"active\":true}" > /dev/null
newid=$(json 'd["id"]' < "$out/body-S08a.json")
call S09a PUT "/api/admin/products/$newid" "$ADMIN" '{"name":"Sonda revisada","price":11.00,"active":true}' > /dev/null
call S09b PUT "/api/admin/products/$newid/" "$ADMIN" '{"name":"Sonda revisada","price":11.00,"active":true}' > /dev/null
call S10a PATCH "/api/admin/products/$newid/stock" "$ADMIN" '{"stockQuantity":9}' > /dev/null
call S10b PATCH "/api/admin/products/$newid/stock/" "$ADMIN" '{"stockQuantity":9}' > /dev/null
call S11a POST "/api/orders" "$CUST" "{\"items\":[{\"productId\":$newid,\"quantity\":1}]}" > /dev/null
call S11b POST "/api/orders/" "$CUST" "{\"items\":[{\"productId\":$newid,\"quantity\":1}]}" > /dev/null
oid=$(json 'd["id"]' < "$out/body-S11a.json")
call S12a GET "/api/orders/$oid" "$CUST"; call S12b GET "/api/orders/$oid/" "$CUST"
call S13b POST "/api/orders/$oid/cancel/" "$CUST" "" > /dev/null
call S13a POST "/api/orders/$oid/cancel" "$CUST" "" > /dev/null
# CSRF: token publicado x cookie, rotação após escrita autenticada e reuso do token antigo
cj="$out/jar-csrf.txt"; rm -f "$cj"
curl -s -D "$out/csrf-h1.txt" -o "$out/csrf-b1.json" -c "$cj" -b "$cj" -u "$ADMIN" "$base/api/csrf" > /dev/null
t1=$(json 'd["token"]' < "$out/csrf-b1.json")
c1=$(awk '$6=="XSRF-TOKEN"{print $7}' "$cj" | tail -1)
record C01 GET /api/csrf "$(head -1 "$out/csrf-h1.txt" | awk '{print $2}')" "token==cookie:$([ "$t1" = "$c1" ] && echo sim || echo não); cookie_emitido:$([ -n "$c1" ] && echo sim || echo não)"
code=$(curl -s -D "$out/csrf-h2.txt" -o /dev/null -w '%{http_code}' -c "$cj" -b "$cj" -u "$ADMIN" -X PATCH \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $t1" --data '{"stockQuantity":8}' "$base/api/admin/products/$newid/stock")
c2=$(awk '$6=="XSRF-TOKEN"{print $7}' "$cj" | tail -1)
setc=$(grep -ci '^set-cookie: XSRF-TOKEN' "$out/csrf-h2.txt" || true)
record C02 PATCH "/api/admin/products/{id}/stock" "$code" "set-cookie_xsrf:$setc; cookie_mudou:$([ "$c1" != "$c2" ] && echo sim || echo não)"
code=$(curl -s -o /dev/null -w '%{http_code}' -c "$cj" -b "$cj" -u "$ADMIN" -X PATCH \
  -H 'Content-Type: application/json' -H "X-XSRF-TOKEN: $t1" --data '{"stockQuantity":7}' "$base/api/admin/products/$newid/stock")
record C03 PATCH "/api/admin/products/{id}/stock (token antigo, cookie atualizado)" "$code" ""
code=$(curl -s -o /dev/null -w '%{http_code}' -u "$ADMIN" -X PATCH -H 'Content-Type: application/json' \
  -H "X-XSRF-TOKEN: probe" -H "Cookie: XSRF-TOKEN=probe" --data '{"stockQuantity":6}' "$base/api/admin/products/$newid/stock")
record C04 PATCH "/api/admin/products/{id}/stock (cookie=cabeçalho escolhidos pelo cliente)" "$code" ""
code=$(curl -s -o /dev/null -w '%{http_code}' -u "$ADMIN" -X PATCH -H 'Content-Type: application/json' \
  --data '{"stockQuantity":6}' "$base/api/admin/products/$newid/stock")
record C05 PATCH "/api/admin/products/{id}/stock (sem CSRF)" "$code" ""
{ echo "label=$label"; echo "jar=$jar"; sha256sum "$jar"; unzip -p "$jar" META-INF/MANIFEST.MF | grep -i Spring-Boot-Version; } > "$out/run.txt"
echo "ok $label -> $results"
