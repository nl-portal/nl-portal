#!/bin/bash

# Apply database migrations
>&2 echo "Apply database migrations"
python src/manage.py migrate
exists=$(echo "from django.contrib.auth import get_user_model; User = get_user_model(); print(User.objects.filter(username='admin').exists())" | python src/manage.py shell)
if [ "False" = "${exists}" ]
then
  echo "Creating user 'admin'"
  python src/manage.py createsuperuser --username=admin --email=admin@example.com --noinput
else
  echo "User 'admin' already exists"
fi
echo "Setting 'admin' password."
echo "from django.contrib.auth import get_user_model; User = get_user_model(); user = User.objects.get(username='admin'); user.set_password('admin'); user.save()" | python src/manage.py shell
echo "Loading fixtures"
#python src/manage.py loaddata demodata
#echo "Loading UPL from UPL-actueel.csv"
#python src/manage.py load_upl --file UPL-actueel.csv
echo "Finished setup"

export PGHOST="${DB_HOST:-db}"
export PGPORT="${DB_PORT:-5432}"
/wait_for_db.sh

export OTEL_SERVICE_NAME="${OTEL_SERVICE_NAME:-openproduct}"
export UWSGI_MAX_REQUESTS="${UWSGI_MAX_REQUESTS:-1000}"

uwsgi_port="${UWSGI_PORT:-8000}"
uwsgi_processes="${UWSGI_PROCESSES:-4}"
uwsgi_threads="${UWSGI_THREADS:-4}"
mountpoint="${SUBPATH:-/}"
unset UWSGI_PORT

echo "Starting server on :${uwsgi_port}"
exec uwsgi \
    --strict \
    --ini /uwsgi.ini \
    --http ":${uwsgi_port}" \
    --http-keepalive \
    --manage-script-name \
    --mount "${mountpoint}"=openproduct.wsgi:application \
    --static-map /static=/app/static \
    --static-map /media=/app/media \
    --chdir src \
    --enable-threads \
    --single-interpreter \
    --die-on-term \
    --need-app \
    --processes "${uwsgi_processes}" \
    --threads "${uwsgi_threads}" \
    --post-buffering=8192 \
    --buffer-size=65535
