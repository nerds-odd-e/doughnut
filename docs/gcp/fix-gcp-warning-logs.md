# Fixing GCP Warning Logs

This document explains how to fix the warning logs appearing in GCP MIG serial console logs.

## Issue 1: Cloud Logging Permission Denied

### Problem
The Google Guest Agent is trying to write logs to Cloud Logging but doesn't have the required permission:
```
Cloud Logging Client Error: rpc error: code = PermissionDenied desc = Permission 'logging.logEntries.create' denied
```

### Root Cause
The service accounts attached to VM instances (`doughnut-gcp-svc-acct` and the default compute service account) don't have the `roles/logging.logWriter` IAM role.

### Solution
Run the script to grant logging permissions:

```bash
./infra/gcp/scripts/grant-logging-permission-to-service-account.sh
```

This script grants the `roles/logging.logWriter` role to both:
- `doughnut-gcp-svc-acct@carbon-syntax-298809.iam.gserviceaccount.com`
- `carbon-syntax-298809-compute@developer.gserviceaccount.com`

### After Running the Script
- **Existing instances**: The warnings will stop after instances are restarted or replaced
- **New instances**: Will automatically have the permission and won't show warnings

## Issue 2: Expired SSH Keys

### Problem
Repeated warnings about expired SSH keys:
```
Invalid user "yeongsheng" or key "...expireOn":"2021-04-16T09:47:16+0000"..." in metadata: invalid ssh key entry - expired key.
```

### Root Cause
Expired SSH keys are still present in the GCP project metadata. The Google Guest Agent checks these periodically and logs warnings.

### Solution
These warnings are **informational only** and don't affect functionality. To remove them:

1. Go to GCP Console → Compute Engine → Metadata → SSH Keys
2. Remove expired keys for user "yeongsheng" (expired on 2021-04-16)
3. Or use gcloud CLI:
   ```bash
   gcloud compute project-info describe --format="value(commonInstanceMetadata.items[ssh-keys].value)" > /tmp/ssh-keys.txt
   # Edit /tmp/ssh-keys.txt to remove expired keys
   gcloud compute project-info add-metadata --metadata-from-file ssh-keys=/tmp/ssh-keys.txt
   ```

### Note
These warnings don't impact the application and can be safely ignored if you don't need to clean them up.

