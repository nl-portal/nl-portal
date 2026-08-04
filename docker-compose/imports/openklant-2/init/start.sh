#!/bin/sh

set -ex

export PGHOST=${DB_HOST:-db}
export PGPORT=${DB_PORT:-5432}

uwsgi_port=${UWSGI_PORT:-8000}
uwsgi_processes=${UWSGI_PROCESSES:-4}
uwsgi_threads=${UWSGI_THREADS:-4}
mountpoint=${SUBPATH:-/}
unset UWSGI_PORT

until pg_isready; do
  >&2 echo "Waiting for database connection..."
  sleep 1
done

>&2 echo "Database is up."

export OTEL_SERVICE_NAME="${OTEL_SERVICE_NAME:-openklant}"

>&2 echo "Apply database migrations"
OTEL_SDK_DISABLED=True python src/manage.py migrate

export UWSGI_MAX_REQUESTS=${UWSGI_MAX_REQUESTS:-1000}

>&2 echo "Starting server on :${uwsgi_port}"
exec uwsgi \
    --strict \
    --ini /uwsgi.ini \
    --http :$uwsgi_port \
    --http-keepalive \
    --manage-script-name \
    --mount $mountpoint=openklant.wsgi:application \
    --static-map /static=/app/static \
    --static-map /media=/app/media \
    --chdir src \
    --enable-threads \
    --single-interpreter \
    --die-on-term \
    --need-app \
    --processes $uwsgi_processes \
    --threads $uwsgi_threads \
    --post-buffering=8192 \
    --buffer-size=65535
