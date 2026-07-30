#!/bin/bash

echo ">>>>  NL Portal init script: Open Notificaties <<<<"
sleep 2
while true
do
    if pg_isready -h $DB_HOST -q && python /app/src/manage.py migrate --check >/dev/null 2>&1
        then
            echo "Database ready."
            echo "Loading fixtures:"
            sleep 2
            python /app/src/manage.py loaddata configuration kanalen
            break
        else
            echo "Database is not ready. Retrying in 10 seconds."
            sleep 10
    fi
done

echo ">>>> Done <<<<"
