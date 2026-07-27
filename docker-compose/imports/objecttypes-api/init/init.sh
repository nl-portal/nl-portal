#!/bin/bash

echo ">>>>  NL Portal init script: Objecttypes API <<<<"
sleep 2
while true
do
    if pg_isready -h $DB_HOST -q && python /app/src/manage.py migrate --check >/dev/null 2>&1
        then
            echo "Database ready."
            echo "Attempting to create admin user:"
            sleep 2
            DJANGO_SUPERUSER_PASSWORD=admin python /app/src/manage.py createsuperuser --username=admin --email=admin@example.com --noinput
            echo "Loading fixtures:"
            sleep 2
            python /app/src/manage.py loaddata configuration objecttypes
            break
        else
            echo "Database is not ready. Retrying in 10 seconds."
            sleep 10
    fi
done

echo ">>>> Done <<<<"
