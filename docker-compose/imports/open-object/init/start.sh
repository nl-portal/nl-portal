#!/bin/sh

set -ex

export PGHOST=${DB_HOST:-db}
export PGPORT=${DB_PORT:-5432}

fixtures_dir=${FIXTURES_DIR:-/app/fixtures}

uwsgi_port=${UWSGI_PORT:-8000}
uwsgi_processes=${UWSGI_PROCESSES:-4}
uwsgi_threads=${UWSGI_THREADS:-4}
mountpoint=${SUBPATH:-/}
unset UWSGI_PORT

/wait_for_db.sh

>&2 echo "Database is up."

export OTEL_SERVICE_NAME="${OTEL_SERVICE_NAME:-objects}"

>&2 echo "Apply database migrations"
OTEL_SDK_DISABLED=True python src/manage.py migrate

if [ -d $fixtures_dir ]; then
    echo "Loading fixtures from $fixtures_dir"
    for fixture in $(ls "$fixtures_dir/"*.json)
    do
        echo "Loading fixture $fixture"
        python src/manage.py loaddata $fixture
    done
fi

if [ -n "${OBJECTS_SUPERUSER_USERNAME}" ]; then
    python src/manage.py createinitialsuperuser \
        --no-input \
        --username "${OBJECTS_SUPERUSER_USERNAME}" \
        --email "${OBJECTS_SUPERUSER_EMAIL:-admin@admin.org}"
    unset OBJECTS_SUPERUSER_USERNAME OBJECTS_SUPERUSER_EMAIL OBJECTS_SUPERUSER_PASSWORD
fi

>&2 echo "Starting server on :${uwsgi_port}"
exec uwsgi \
    --strict \
    --ini /uwsgi.ini \
    --http :$uwsgi_port \
    --http-keepalive \
    --manage-script-name \
    --mount $mountpoint=objects.wsgi:application \
    --static-map /static=/app/static \
    --static-map /media=/app/media \
    --chdir src \
    --enable-threads \
    --single-interpreter \
    --die-on-term \
    --need-app \
    --processes $uwsgi_processes \
    --threads $uwsgi_threads \
    --buffer-size=65535
