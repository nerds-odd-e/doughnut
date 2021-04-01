#!/bin/bash
SCRIPTPATH="$(
        cd "$(dirname "$0")" > /dev/null 2>&1
        pwd -P
)"

URL_MAP_VALIDATION_PASSED=$(gcloud compute url-maps validate --source doughnut-app-http-redirect-https-web-map.yaml | grep 'testPassed: true' | wc -l | xargs)

if [ ${URL_MAP_VALIDATION_PASSED} -eq 1 ]; then
  ${SCRIPTPATH}/create-http-https-redirect-urlmap.sh
else
  echo "Supplied URL map yaml failed validation!"
fi
