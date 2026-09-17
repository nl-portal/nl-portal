#!/bin/bash
set -e

sh /wait_for_db.sh

>&2 echo "Apply database migrations"
python src/manage.py migrate

echo "Loading fixtures"
python src/manage.py loaddata admin_user configuration partijen klantcontacten contactgegevens
echo "Finished setup"

sh /start.sh
