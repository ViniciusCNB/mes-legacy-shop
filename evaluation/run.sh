#!/usr/bin/env bash
set -eo pipefail
repo=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo"
if [ -z "$JAVA_HOME" ]; then echo 'Defina JAVA_HOME para um JDK 17.' >&2; exit 1; fi
if [ "$#" -gt 0 ]; then app_jar=$1; else app_jar="$repo/target/mes-legacy-shop-0.0.1-SNAPSHOT.jar"; fi
test -f "$app_jar" || { echo "JAR não encontrado: $app_jar" >&2; exit 1; }
port=$EVALUATION_HTTP_PORT
if [ -z "$port" ]; then port=18080; fi
case "$port" in *[!0-9]*|'') echo 'Porta inválida.' >&2; exit 1;; esac
if [ "$port" -lt 1024 ] || [ "$port" -gt 65535 ]; then echo 'Porta inválida.' >&2; exit 1; fi
if (echo > "/dev/tcp/127.0.0.1/$port") >/dev/null 2>&1; then
  echo "Porta HTTP $port ocupada; escolha EVALUATION_HTTP_PORT." >&2; exit 1
fi
java_version=$("$JAVA_HOME/bin/java" -XshowSettings:properties -version 2>&1 | sed -n 's/.*java.version = //p' | head -1)
java_version=$(printf '%s' "$java_version" | tr -d '\r')
case "$java_version" in 17.*) ;; *) echo 'JAVA_HOME deve selecionar JDK 17.' >&2; exit 1;; esac
project="mes-evaluation-$(od -An -N6 -tx1 /dev/urandom | tr -d ' \n\r')"
[[ "$project" =~ ^mes-evaluation-[0-9a-f]{12}$ ]] || exit 1
artifacts="$repo/target/evaluation-runs/$project"
mkdir -p "$artifacts"
compose=(docker compose -f "$repo/evaluation/compose.yaml" -p "$project")
application_pid=''
owns_project=false
passed=false
evaluation_started=false
cleanup() {
  result=$?
  trap - EXIT
  if [ "$evaluation_started" = true ] && [ -d evaluation/target/surefire-reports ]; then
    cp -R evaluation/target/surefire-reports "$artifacts/surefire-reports" || result=1
  fi
  if [ -n "$application_pid" ] && kill -0 "$application_pid" 2>/dev/null; then
    kill "$application_pid" || result=1
    wait "$application_pid" 2>/dev/null || true
  fi
  if [ "$owns_project" = true ]; then
    "${compose[@]}" logs --no-color > "$artifacts/postgres.log" 2>&1 || result=1
    "${compose[@]}" down --volumes || result=1
  fi
  printf 'passed=%s\nexit=%s\n' "$passed" "$result" > "$artifacts/result.txt"
  echo "Evidências: $artifacts"
  exit "$result"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
existing=$(docker ps -aq --filter "label=com.docker.compose.project=$project")
test -z "$existing" || { echo "Projeto já existe: $project" >&2; exit 1; }
existing_volumes=$(docker volume ls -q --filter "label=com.docker.compose.project=$project")
test -z "$existing_volumes" || { echo "Volume já existe para $project" >&2; exit 1; }
owns_project=true
"${compose[@]}" up -d --wait
binding=$("${compose[@]}" port postgres 5432 | tr -d '\r')
database_port=$(printf '%s\n' "$binding" | sed 's/.*://')
"${compose[@]}" exec -T postgres psql -v ON_ERROR_STOP=1 -U shop_evaluation -d shop_evaluation < evaluation/fixtures/seed.sql
POSTGRES_HOST=127.0.0.1 POSTGRES_PORT="$database_port" POSTGRES_DB=shop_evaluation \
POSTGRES_USER=shop_evaluation POSTGRES_PASSWORD=evaluation_local_only SERVER_PORT="$port" \
SPRING_PROFILES_ACTIVE=evaluation "$JAVA_HOME/bin/java" -jar "$app_jar" > "$artifacts/application.log" 2>&1 &
application_pid=$!
ready=false
for attempt in $(seq 1 90); do
  kill -0 "$application_pid" 2>/dev/null || { echo 'Aplicação encerrou; consulte application.log.' >&2; exit 1; }
  if curl --fail --silent --show-error --max-time 2 "http://127.0.0.1:$port/actuator/health" > "$artifacts/health.json" 2>/dev/null; then
    if grep -q '"status":"UP"' "$artifacts/health.json"; then ready=true; break; fi
  fi
  sleep 1
done
test "$ready" = true || { echo 'Saúde não ficou UP em 90s.' >&2; exit 1; }
{
  printf 'project=%s\nbaseUrl=http://127.0.0.1:%s\ndatabasePort=%s\njavaVersion=%s\n' "$project" "$port" "$database_port" "$java_version"
  printf 'sourceCommit=%s\n' "$(git rev-parse HEAD)"
  printf 'sourceDirty=%s\n' "$(test -z "$(git status --porcelain)" && echo false || echo true)"
  if [ -f evaluation/SNAPSHOT.sha256 ]; then sha256sum evaluation/SNAPSHOT.sha256; fi
  printf 'evaluatorVersion=1.0.0\napplicationJar=%s\n' "$app_jar"
  sha256sum "$app_jar"
} > "$artifacts/run.txt"
evaluation_started=true
if sh ./mvnw -B -ntp -f evaluation/pom.xml clean verify "-Devaluation.baseUrl=http://127.0.0.1:$port" > "$artifacts/evaluation.log" 2>&1; then
  passed=true
  tail -35 "$artifacts/evaluation.log"
else
  maven_result=$?
  tail -60 "$artifacts/evaluation.log"
  exit "$maven_result"
fi
