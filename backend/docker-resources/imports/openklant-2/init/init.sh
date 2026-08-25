#!/bin/bash
set -e

>&2 echo "Apply database migrations"
python src/manage.py migrate

echo "Loading fixtures"
python src/manage.py loaddata configuration partijen klantcontacten contactgegevens
echo "Finished setup"

sh /start.sh
