#/bin/sh

gcloud compute instances create doughnut-instance \
  --image-family debian-10 \
  --image-project debian-cloud \
  --machine-type g1-small \
  --scopes "userinfo-email,cloud-platform" \
  --metadata-from-file startup-script=instance-startup.sh \
  --metadata BUCKET=dough-01 \
  --zone us-east1-b \
  --tags http-server
