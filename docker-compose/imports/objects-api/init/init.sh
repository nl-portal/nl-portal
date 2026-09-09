#!/bin/bash
echo ">>>>  NL Portal init script: Objects API <<<<"
until python /app/src/manage.py migrate --check >/dev/null 2>&1; do
    echo "Waiting for objects-api migrations..."
    sleep 5
done
python /app/src/manage.py loaddata configuration objecttypes objects
echo ">>>> Done <<<<"
