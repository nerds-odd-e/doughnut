# Finding Spring Boot Logs in GCP Cloud Logging

## Overview

Spring Boot application logs written to stdout/stderr are automatically captured by the Google Cloud Logging agent on GCE instances. However, you need to know where to look for them.

## Where to Find Logs

### In GCP Console

1. Go to **Cloud Logging** → **Logs Explorer**
2. Use these filters to find your application logs:

**Option 1: Filter by Resource Type**
```
resource.type="gce_instance"
resource.labels.instance_id="<instance-id>"
```

**Option 2: Filter by Log Name**
```
resource.type="gce_instance"
logName="projects/carbon-syntax-298809/logs/stdout"
```

**Option 3: Filter by Text Content**
```
resource.type="gce_instance"
textPayload=~"com.odde.doughnut"
```

### Using gcloud CLI

```bash
# View logs for a specific instance
gcloud logging read "resource.type=gce_instance AND resource.labels.instance_id=<instance-id> AND logName=~stdout" --limit=50 --format=json

# Tail logs for a specific instance
gcloud logging tail "resource.type=gce_instance AND resource.labels.instance_id=<instance-id> AND logName=~stdout"
```

### Finding Instance IDs

```bash
# Get instance IDs from MIG health check
./infra/gcp/scripts/check-mig-doughnut-app-service-health.sh

# Or list all instances
gcloud compute instances list --filter="tags:mig-app-srv"
```

## Log Configuration

The production profile now includes proper logging configuration:

- **Logback**: Logs to CONSOLE appender (stdout/stderr)
- **Log Levels**: 
  - Root: INFO
  - Application (`com.odde.doughnut`): INFO
  - Spring Framework: INFO
  - Hibernate: WARN
  - HikariCP: WARN

## Troubleshooting

### Logs Not Appearing?

1. **Check Service Account Permissions**
   ```bash
   ./infra/gcp/scripts/grant-logging-permission-to-service-account.sh
   ```

2. **Verify Application is Running**
   ```bash
   # SSH into instance and check
   ps aux | grep java
   ```

3. **Check if Logs are Being Written**
   ```bash
   # SSH into instance and check stdout/stderr
   journalctl -u google-startup-scripts.service
   ```

4. **Verify Cloud Logging Agent**
   ```bash
   # Check if agent is running
   systemctl status google-fluentd
   # Or on newer images
   systemctl status google-cloud-ops-agent
   ```

### Log Format

Logs appear in GCP with:
- **Resource Type**: `gce_instance`
- **Log Name**: `projects/<project-id>/logs/stdout` or `.../logs/stderr`
- **Payload**: Text format (from Logback CONSOLE appender)

## Notes

- Logs written to stdout/stderr are automatically captured
- No additional configuration needed beyond proper logback setup
- Logs may take a few seconds to appear in Cloud Logging
- The Cloud Logging agent runs automatically on GCE instances

