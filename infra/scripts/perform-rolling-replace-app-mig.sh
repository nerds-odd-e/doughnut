#!/bin/bash
gcloud compute instance-groups managed rolling-action replace doughnut-app-group --max-unavailable=1 --zone=us-east1-b
