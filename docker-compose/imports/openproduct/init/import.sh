#!/bin/bash

until pg_isready -h "$DB_HOST" -q && python /app/src/manage.py migrate --check >/dev/null 2>&1; do
  echo "Waiting for openproduct migrations..."
  sleep 2
done

DJANGO_SUPERUSER_PASSWORD=admin python /app/src/manage.py createsuperuser --username=admin --email=admin@example.com --noinput || true
python /app/src/manage.py load_upl --file /app/init/UPL-actueel.csv
python /app/src/manage.py loaddata configuration demodata
echo "openproduct import finished"
