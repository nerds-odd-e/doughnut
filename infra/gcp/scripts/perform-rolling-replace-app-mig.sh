#!/bin/bash
gcloud compute instance-groups managed rolling-action start-update doughnut-app-group --minimal-action replace --most-disruptive-allowed-action replace --max-surge 1 --max-unavailable 0 --zone us-east1-b --version='template=doughnut-app-debian12-zulu21-openai-mig-template'
