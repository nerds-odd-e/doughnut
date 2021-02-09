#!/bin/bash
gcloud compute instances tail-serial-port-output doughnut-app-instance --zone us-east1-b
