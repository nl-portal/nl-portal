#!/bin/bash
set -e
echo ">>>>  NL Portal init script: Open Klant 2 <<<<"
python /app/src/manage.py loaddata configuration partijen klantcontacten
echo ">>>> Done <<<<"
