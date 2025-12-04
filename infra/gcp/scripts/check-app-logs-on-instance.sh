#!/bin/bash
# Script to check if Spring Boot logs are being written on an instance
# Usage: ./check-app-logs-on-instance.sh <instance-id>

INSTANCE_ID=${1:-""}

if [ -z "$INSTANCE_ID" ]; then
  echo "Usage: $0 <instance-id>"
  echo ""
  echo "Get instance ID from:"
  echo "  ./infra/gcp/scripts/check-mig-doughnut-app-service-health.sh"
  exit 1
fi

ZONE="us-east1-b"

echo "Checking logs on instance: $INSTANCE_ID"
echo ""

# Check startup script logs (stdout/stderr from startup)
echo "1. Checking startup script logs (last 50 lines):"
gcloud compute ssh $INSTANCE_ID --zone=$ZONE --command="journalctl -u google-startup-scripts.service --no-pager -n 50 2>&1 | tail -30 || echo 'Cannot read startup logs'"

echo ""
echo "2. Checking if Java process stdout/stderr are connected:"
gcloud compute ssh $INSTANCE_ID --zone=$ZONE --command="ps aux | grep java | grep -v grep && echo 'Java process found - stdout/stderr should be captured by Cloud Logging agent' || echo 'No Java process found'"

echo ""
echo "3. Checking if Java process is running:"
gcloud compute ssh $INSTANCE_ID --zone=$ZONE --command="ps aux | grep java | grep -v grep || echo 'No Java process found'"

echo ""
echo "4. Checking nohup.out (if exists):"
gcloud compute ssh $INSTANCE_ID --zone=$ZONE --command="ls -lh nohup.out 2>&1 | head -5 || echo 'No nohup.out found'"

echo ""
echo "5. Checking Cloud Logging agent status:"
gcloud compute ssh $INSTANCE_ID --zone=$ZONE --command="systemctl status google-cloud-ops-agent 2>&1 | head -10 || systemctl status google-fluentd 2>&1 | head -10 || echo 'Cloud Logging agent not found'"

echo ""
echo "6. Checking if application is responding:"
gcloud compute ssh $INSTANCE_ID --zone=$ZONE --command="curl -s http://localhost:8081/api/healthcheck 2>&1 | head -5 || echo 'Health check failed'"

echo ""
echo "7. Checking startup script output (should contain Spring Boot logs):"
gcloud compute ssh $INSTANCE_ID --zone=$ZONE --command="journalctl -u google-startup-scripts.service --no-pager 2>&1 | grep -i 'spring\|doughnut\|started\|pid' | tail -20 || echo 'No Spring Boot logs in startup script output'"

echo ""
echo "8. Checking for any Java errors:"
gcloud compute ssh $INSTANCE_ID --zone=$ZONE --command="journalctl -u google-startup-scripts.service --no-pager 2>&1 | grep -i 'error\|exception\|failed' | tail -10 || echo 'No errors found'"

