#!/usr/bin/env bash
# Starts the template pages demo (#170): the supporting services in RUN_MODE=demo (ZGW, Haalcentraal,
# IKO and WireMock), the app-dev backend with the Spring profile `demo` and the app-dev frontend,
# both from this repo's sources. The backend and frontend run in the background and log to $LOG_DIR.
# See DEMO.md.
#
# Usage: ./run-demo.sh [up|down|logs|ps]   (default: up)
#        ./run-demo.sh logs backend|frontend|<container>
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$ROOT_DIR/docker-compose"
LOG_DIR="${LOG_DIR:-${TMPDIR:-/tmp}/nl-portal-demo}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-600}"

export RUN_MODE=demo

compose() {
    docker compose --project-directory "$COMPOSE_DIR" -f "$COMPOSE_DIR/docker-compose.yaml" \
        --profile zgw --profile haalcentraal --profile clamav "$@"
}

require() {
    command -v docker >/dev/null || { echo "docker is not installed" >&2; exit 1; }
    docker compose version >/dev/null 2>&1 || { echo "docker compose plugin is not installed" >&2; exit 1; }
    docker info >/dev/null 2>&1 || { echo "docker daemon is not running" >&2; exit 1; }
    command -v java >/dev/null || { echo "java 21 is not installed" >&2; exit 1; }
    command -v pnpm >/dev/null || { echo "pnpm is not installed (npm install -g pnpm)" >&2; exit 1; }
    command -v lsof >/dev/null || { echo "lsof is not installed" >&2; exit 1; }
}

port_free() {
    local port="$1"
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
        echo "Port $port is in use. Stop that process first, or run: ./run-demo.sh down" >&2
        exit 1
    fi
}

wait_for() {
    local name="$1" url="$2" start=$SECONDS
    printf "Waiting for %s (%s) " "$name" "$url"
    until curl -s -o /dev/null "$url"; do
        if (( SECONDS - start > TIMEOUT_SECONDS )); then
            echo " timed out after ${TIMEOUT_SECONDS}s. Check: ./run-demo.sh logs" >&2
            exit 1
        fi
        printf "."
        sleep 5
    done
    echo " up"
}

check_crashed() {
    local crashed
    crashed="$(compose ps -a --format '{{.Name}} {{.State}} {{.ExitCode}}' | awk '$2 == "exited" && $3 != 0 {print $1}')"
    if [[ -n "$crashed" ]]; then
        echo "These containers crashed:" >&2
        echo "$crashed" >&2
        echo "Inspect with: ./run-demo.sh logs <name>" >&2
        exit 1
    fi
}

start_app() {
    local name="$1" dir="$2"
    shift 2
    (cd "$dir" && nohup "$@" >"$LOG_DIR/$name.log" 2>&1 & echo $! >"$LOG_DIR/$name.pid")
}

kill_tree() {
    local pid="$1" child
    for child in $(pgrep -P "$pid" 2>/dev/null); do
        kill_tree "$child"
    done
    kill "$pid" 2>/dev/null || true
}

stop_app() {
    local pidfile="$LOG_DIR/$1.pid"
    [[ -f "$pidfile" ]] || return 0
    kill_tree "$(cat "$pidfile")"
    rm -f "$pidfile"
}

up() {
    require
    stop_app backend
    stop_app frontend
    port_free 8080
    port_free 3000
    mkdir -p "$LOG_DIR"

    compose down --remove-orphans
    compose up -d
    wait_for "keycloak" "http://localhost:8082"
    wait_for "IKO" "http://localhost:8084"
    check_crashed

    start_app backend "$ROOT_DIR/backend" \
        ./gradlew --no-daemon :app-dev:bootRun --args='--spring.profiles.active=demo'
    (cd "$ROOT_DIR/frontend" && pnpm install --frozen-lockfile >"$LOG_DIR/frontend-install.log" 2>&1) || {
        echo "pnpm install failed, see $LOG_DIR/frontend-install.log" >&2
        exit 1
    }
    start_app frontend "$ROOT_DIR/frontend" pnpm dev

    wait_for "backend" "http://localhost:8080/actuator/health"
    wait_for "frontend" "http://localhost:3000"
    check_crashed

    cat <<EOF

Template pages demo is running:
  Portal frontend      http://localhost:3000   (login: burger/burger or bedrijf/bedrijf)
  Backend              http://localhost:8080
  Keycloak             http://localhost:8082
  Open Object          http://localhost:8010   (the 'contact' template object)
  OpenProduct          http://localhost:8015
  IKO                  http://localhost:8084
  WireMock             http://localhost:8083

Demo pages (see DEMO.md):
  /over-ons              static, file template, alert and link list
  /contact               static, template from the Objects API
  /erfpacht              dynamic, OpenProduct list with status tabs and paging; rows open /erfpacht/<id>
  /parkeervergunningen   dynamic, OpenProduct parking permits; rows open /parkeervergunningen/<id>
  /vergunningen          dynamic, IKO profiles (burger only)

Logs: ./run-demo.sh logs backend|frontend|<container>  ($LOG_DIR)
Stop with: ./run-demo.sh down
EOF
}

down() {
    stop_app backend
    stop_app frontend
    compose down
}

logs() {
    case "${1:-}" in
        backend|frontend) tail -f "$LOG_DIR/$1.log" ;;
        *) compose logs -f "$@" ;;
    esac
}

case "${1:-up}" in
    up)   up ;;
    down) down ;;
    logs) logs "${@:2}" ;;
    ps)   compose ps ;;
    *)    echo "Usage: $0 [up|down|logs|ps]" >&2; exit 1 ;;
esac
