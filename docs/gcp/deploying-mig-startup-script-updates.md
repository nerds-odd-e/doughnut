# Deploying MIG Startup Script Updates

This guide explains how to deploy updated startup scripts to your Managed Instance Group (MIG) instances.

## Quick Deploy (Automated)

Use the automated script to update the startup script:

```bash
cd infra/gcp/scripts
./update-mig-startup-script.sh
```

This script will:
1. Create a new instance template with the updated startup script
2. Update the MIG to use the new template
3. Start a rolling replacement of instances
4. Provide commands to monitor the rollout

## Manual Steps

If you prefer to do it manually or need more control:

### Step 1: Create New Instance Template

```bash
cd infra/gcp/scripts

# Create new template with updated startup script
gcloud compute instance-templates create doughnut-app-debian12-zulu25-openai-mig-template-$(date +%Y%m%d-%H%M%S) \
  --image doughnut-debian12-zulu25-mysql84-base-saltstack \
  --service-account 220715781008-compute@developer.gserviceaccount.com \
  --service-account doughnut-gcp-svc-acct@carbon-syntax-298809.iam.gserviceaccount.com \
  --scopes https://www.googleapis.com/auth/cloud-platform \
  --scopes "userinfo-email,cloud-platform" \
  --machine-type e2-medium \
  --metadata-from-file startup-script=./mig-zulu25-openai-app-instance-startup.sh \
  --metadata BUCKET=dough-01 \
  --tags mig-app-srv
```

### Step 2: Update MIG to Use New Template

```bash
# Replace NEW_TEMPLATE_NAME with the template name from Step 1
gcloud compute instance-groups managed set-instance-template doughnut-app-group \
  --template=NEW_TEMPLATE_NAME \
  --zone=us-east1-b
```

### Step 3: Start Rolling Replacement

```bash
# This will replace instances one at a time
gcloud compute instance-groups managed rolling-action replace doughnut-app-group \
  --max-surge 0 \
  --max-unavailable 1 \
  --zone=us-east1-b
```

### Step 4: Monitor Rollout

```bash
# Check if rollout is stable
./infra/gcp/scripts/check-mig-rollout.sh

# Or check health status
./infra/gcp/scripts/check-mig-doughnut-app-service-health.sh
```

## Verification

After deployment, verify the new startup script is working:

1. **Check logs are being written:**
   ```bash
   # SSH into an instance and check
   tail -f /var/log/doughnut-app.log
   ```

2. **Check logs in GCP Cloud Logging:**
   - Go to Cloud Logging → Logs Explorer
   - Filter: `logName=~"doughnut-app"`
   - You should see Spring Boot application logs

3. **Verify application is running:**
   ```bash
   # Check health endpoint
   curl https://dough.odd-e.com/api/healthcheck
   ```

## Rollback (If Needed)

If something goes wrong, you can rollback to the previous template:

```bash
# Find previous template
gcloud compute instance-templates list --filter="name~doughnut-app-debian12-zulu25-openai-mig-template"

# Update MIG to use previous template
gcloud compute instance-groups managed set-instance-template doughnut-app-group \
  --template=PREVIOUS_TEMPLATE_NAME \
  --zone=us-east1-b

# Start rolling replacement
gcloud compute instance-groups managed rolling-action replace doughnut-app-group \
  --max-surge 0 \
  --max-unavailable 1 \
  --zone=us-east1-b
```

## Important Notes

- **Rolling replacement**: Instances are replaced one at a time to maintain availability
- **No downtime**: With `--max-unavailable 1`, at least one instance remains available
- **Template cleanup**: Old templates are not automatically deleted. Clean them up periodically:
  ```bash
  gcloud compute instance-templates delete OLD_TEMPLATE_NAME
  ```
- **Startup script location**: The startup script must be in `infra/gcp/scripts/` directory
- **Changes take effect**: New instances will use the updated startup script immediately

## Troubleshooting

### Rollout Not Completing

If the rollout doesn't complete:
1. Check instance health: `./infra/gcp/scripts/check-mig-doughnut-app-service-health.sh`
2. Check for errors in serial console logs
3. Verify startup script syntax is correct

### Instances Not Starting

If new instances fail to start:
1. Check serial console logs in GCP Console
2. Verify startup script has correct permissions
3. Check that all required secrets are accessible
4. Verify network connectivity to database

### Logs Not Appearing

If logs don't appear in Cloud Logging:
1. Verify service account has logging permissions: `./infra/gcp/scripts/grant-logging-permission-to-service-account.sh`
2. Check log file exists: `tail -f /var/log/doughnut-app.log` (SSH into instance)
3. Verify Cloud Logging agent is running: `systemctl status google-cloud-ops-agent`

