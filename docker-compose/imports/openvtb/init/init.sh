#!/bin/bash
echo ">>>>  NL Portal init script: Open VTB <<<<"
until python /app/src/manage.py migrate --check >/dev/null 2>&1; do
    echo "Waiting for openvtb migrations..."
    sleep 5
done
python /app/src/manage.py loaddata admin_user configuration berichten
echo ">>>> Done <<<<"
