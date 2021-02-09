#!/bin/bash
RUNING_APP_INSTANCE_COUNT=$(gcloud compute instances list --filter='tags:app-server' | grep -E 'RUNNING|TERMINATED' | wc -l | xargs)

if [[ ${RUNING_APP_INSTANCE_COUNT} -eq 0 ]]; then
	exit 0
fi

gcloud compute instances delete doughnut-app-instance --quiet --zone us-east1-b
