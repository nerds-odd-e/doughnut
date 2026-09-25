#!/bin/bash
# Prints the production application's health and its log on an instance.
# See docs/gcp/troubleshooting-springboot-logs.md
# Usage: ./check-app-logs-on-instance.sh <instance-name>

INSTANCE_ID=${1:-""}

if [ -z "$INSTANCE_ID" ]; then
  echo "Usage: $0 <instance-name>"
  echo "Instance names: gcloud compute instances list"
  exit 1
fi

gcloud compute ssh "$INSTANCE_ID" --zone=us-east1-b --command='
echo "== Health check"; curl -s http://localhost:8081/api/healthcheck; echo
echo "== Last 50 lines of /logs/donut-prod.log"; sudo tail -n 50 /logs/donut-prod.log
echo "== ERROR lines"; sudo grep -h ERROR /logs/archived/donut-prod.3.log /logs/archived/donut-prod.2.log /logs/archived/donut-prod.1.log /logs/donut-prod.log 2>/dev/null | tail -n 50
'
