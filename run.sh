#!/usr/bin/env bash
# Starts a full NL Portal test environment: backend + frontend built from this repo
# (RUN_MODE=local) together with all ZGW and Haalcentraal supporting services.
#
# Usage: ./run.sh [up|down|logs|ps]   (default: up)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="$ROOT_DIR/docker-compose"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-600}"

export RUN_MODE=local

compose() {
    docker compose --project-directory "$COMPOSE_DIR" -f "$COMPOSE_DIR/docker-compose.yaml" \
        --profile zgw --profile haalcentraal --profile clamav "$@"
}

require() {
    command -v docker >/dev/null || { echo "docker is not installed" >&2; exit 1; }
    docker compose version >/dev/null 2>&1 || { echo "docker compose plugin is not installed" >&2; exit 1; }
    docker buildx version >/dev/null 2>&1 || {
        echo "docker buildx is not installed (e.g. sudo apt-get install docker-buildx-plugin)" >&2
        exit 1
    }
    docker info >/dev/null 2>&1 || { echo "docker daemon is not running" >&2; exit 1; }
}

wait_for() {
    local name="$1" url="$2" start=$SECONDS
    printf "Waiting for %s (%s) " "$name" "$url"
    until curl -s -o /dev/null "$url"; do
        if (( SECONDS - start > TIMEOUT_SECONDS )); then
            echo " timed out after ${TIMEOUT_SECONDS}s. Check: ./run.sh logs" >&2
            exit 1
        fi
        printf "."
        sleep 5
    done
    echo " up"
}

# Most services share the `localhost` container's network namespace; if it fails to
# attach to the compose network, nothing can resolve service hostnames.
check_network() {
    if ! docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' localhost | grep -q .; then
        echo "The 'localhost' container has no network attached. Run: ./run.sh down && ./run.sh" >&2
        exit 1
    fi
}

check_crashed() {
    local crashed
    crashed="$(compose ps -a --format '{{.Name}} {{.State}} {{.ExitCode}}' | awk '$2 == "exited" && $3 != 0 {print $1}')"
    if [[ -n "$crashed" ]]; then
        echo "These containers crashed:" >&2
        echo "$crashed" >&2
        echo "Inspect with: ./run.sh logs <name>" >&2
        exit 1
    fi
}

up() {
    require
    # Start from a clean slate (volumes are kept) to avoid stale containers/networks
    compose down --remove-orphans
    compose up -d --build
    check_network
    wait_for "backend" "http://localhost:8080/actuator/health"
    wait_for "frontend" "http://localhost:3000"
    check_crashed

    cat <<EOF

NL Portal test environment is running:
  Portal frontend      http://localhost:3000
  Backend GraphQL      http://localhost:8080/graphql
  Keycloak             http://localhost:8082
  Open Zaak            http://localhost:8001
  Open Object          http://localhost:8010
  Open Notificaties    http://localhost:8012
  OpenKlant 2          http://localhost:8014
  OpenProduct          http://localhost:8015
  Haalcentraal BRP     http://localhost:5010
  Haalcentraal Bewon.  http://localhost:5011

Test users: see docker-compose/README.md
Stop with: ./run.sh down
EOF
}

case "${1:-up}" in
    up)   up ;;
    down) compose down ;;
    logs) compose logs -f "${@:2}" ;;
    ps)   compose ps ;;
    *)    echo "Usage: $0 [up|down|logs|ps]" >&2; exit 1 ;;
esac
