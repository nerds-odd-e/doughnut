#!/bin/bash
MIG_INSTANCE_ID=$1
gcloud compute instances get-serial-port-output $MIG_INSTANCE_ID --zone us-east1-b
