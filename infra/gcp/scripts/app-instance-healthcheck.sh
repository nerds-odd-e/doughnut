#!/bin/bash
HEALTHCHECK_STATUS=""
RETRY=30
while [[ ${RETRY} -gt 0 ]] && [[ "${HEALTHCHECK_STATUS}" != *"OK"* ]]
do
  sleep 20
  echo "RETRY (${RETRY})..."
  HEALTHCHECK_STATUS=$(curl -sk https://dough.odd-e.com/api/healthcheck 2>&1)
  if [[ "${HEALTHCHECK_STATUS}" == *"OK"* ]]; then
    echo "doughnut-app responded ${HEALTHCHECK_STATUS}!"
    exit 0
  fi
  RETRY=$((RETRY-1))
done

if [[ "${HEALTHCHECK_STATUS}" != *"OK"* ]]; then
  echo "doughnut-app NOT RESPONDING!"
  exit 1
fi
