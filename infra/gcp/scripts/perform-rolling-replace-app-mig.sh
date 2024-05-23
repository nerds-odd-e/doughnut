#!/bin/bash
gcloud compute instance-groups managed rolling-action start-update doughnut-app-group --minimal-action refresh --most-disruptive-allowed-action refresh --max-surge 1 --max-unavailable 1 --zone us-east1-b --version='template=doughnut-app-debian12-zulu21-openai-mig-template'
