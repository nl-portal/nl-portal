#!/bin/bash
until pg_isready -h "$DB_HOST" -q && python /app/src/manage.py migrate --check >/dev/null 2>&1; do
    echo "Waiting for openproduct migrations..."
    sleep 2
done
python /app/src/manage.py load_upl --file /app/init/UPL-actueel.csv
python /app/src/manage.py loaddata configuration demodata
echo "openproduct import finished"
