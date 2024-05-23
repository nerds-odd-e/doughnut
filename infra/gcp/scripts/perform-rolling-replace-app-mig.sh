#!/bin/bash
gcloud compute instance-groups managed rolling-action start-update doughnut-app-group --minimal-action refresh --most-disruptive-allowed-action refresh --max-surge 1 --max-unavailable 0 --zone us-east1-b --version='template=projects/carbon-syntax-298809/regions/us-east1-b/instanceTemplates/doughnut-app-debian12-zulu21-openai-mig-template'
