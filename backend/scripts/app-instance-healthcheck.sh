#!/bin/sh
HEALTHCHECK_STATUS=""
RETRY=15
while [ ${RETRY} -gt 0 ] && [ "${HEALTHCHECK_STATUS}" != "OK" ]
do
  sleep 20
  HEALTHCHECK_STATUS=$(curl -s https://dough.odd-e.com/api/healthcheck 2>&1)
  echo "RETRY (${RETRY}): ${HEALTHCHECK_STATUS}"
  RETRY=$((RETRY-1))
done

if [ "${HEALTHCHECK_STATUS}" != "OK" ]; then
  echo "doughnut-app NOT RESPONDING!"
  exit 1
else 
  echo "doughnut-app responded ${HEALTHCHECK_STATUS}"
  exit 0
fi
