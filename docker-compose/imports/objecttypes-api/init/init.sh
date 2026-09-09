#!/bin/bash
echo ">>>>  NL Portal init script: Objecttypes API <<<<"
until python /app/src/manage.py migrate --check >/dev/null 2>&1; do
    echo "Waiting for objecttypes-api migrations..."
    sleep 5
done
python /app/src/manage.py loaddata configuration objecttypes
echo ">>>> Done <<<<"
