#!/bin/bash
set -e
echo ">>>>  NL Portal init script: Open Zaak <<<<"
python /app/src/manage.py loaddata configuration catalogi documenten zaken
echo ">>>> Done <<<<"
