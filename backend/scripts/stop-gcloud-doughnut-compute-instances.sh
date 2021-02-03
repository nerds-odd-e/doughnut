#/bin/sh

gcloud compute instances stop \
  doughnut-app-instance doughnut-db-instance \
  --zone us-east1-b
