#!/bin/bash
gcloud compute instance-groups managed rolling-action replace doughnut-app-group --minimal-action restart --most-disruptive-allowed-action replace --max-surge 1 --max-unavailable 0 --zone us-east1-b
