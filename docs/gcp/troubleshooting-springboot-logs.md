# Troubleshooting Spring Boot Logs in GCP Cloud Logging

## Quick Diagnostic

Run this script to check if logs are being produced:

```bash
# Get instance ID first
./infra/gcp/scripts/check-mig-doughnut-app-service-health.sh

# Then check logs
./infra/gcp/scripts/check-app-logs-on-instance.sh <instance-id>
```

## Common Issues

### Issue 1: Application Not Starting

**Symptoms**: No Java process running, no logs at all

**Check**:
```bash
# SSH into instance
gcloud compute ssh <instance-id> --zone=us-east1-b

# Check if Java process exists
ps aux | grep java

# Check startup script logs
journalctl -u google-startup-scripts.service --no-pager | tail -100
```

**Solution**: Check for errors in startup script execution

### Issue 2: Logs Written But Not Appearing in Cloud Logging

**Symptoms**: Application running, logs visible via SSH but not in Cloud Logging

**Possible Causes**:
1. **Service account permissions**: Ensure logging permissions are granted
   ```bash
   ./infra/gcp/scripts/grant-logging-permission-to-service-account.sh
   ```

2. **Cloud Logging agent not running**:
   ```bash
   # SSH into instance and check
   systemctl status google-cloud-ops-agent
   # Or
   systemctl status google-fluentd
   ```

3. **Logs going to wrong location**: Check where logs are actually written
   ```bash
   # SSH into instance
   lsof -p $(pgrep -f "doughnut.*jar") | grep -E "log|out|err"
   ```

### Issue 3: Application Starting But No Application Logs

**Symptoms**: Startup script logs visible, but no Spring Boot application logs

**Check**:
1. **Verify Spring Boot is actually starting**:
   ```bash
   # SSH and check
   curl http://localhost:8081/api/healthcheck
   ```

2. **Check log levels**: Ensure INFO level logs are enabled
   - Check `application.yml` prod profile
   - Check `logback-spring.xml` prod profile

3. **Verify profile is active**:
   ```bash
   # Check environment variables
   ps aux | grep java | grep spring.profiles.active
   ```

## Finding Logs in Cloud Logging

### Method 1: Search by Instance ID

1. Get instance ID:
   ```bash
   ./infra/gcp/scripts/check-mig-doughnut-app-service-health.sh
   ```

2. In GCP Console → Cloud Logging → Logs Explorer:
   ```
   resource.type="gce_instance"
   resource.labels.instance_id="<instance-id>"
   ```

### Method 2: Search by Log Content

```
resource.type="gce_instance"
textPayload=~"com.odde.doughnut"
```

### Method 3: Search Startup Script Output

```
resource.type="gce_instance"
logName="projects/carbon-syntax-298809/logs/stdout"
textPayload=~"Spring Boot\|doughnut\|Started DoughnutApplication"
```

## Expected Log Output

When Spring Boot starts successfully, you should see logs like:

```
Started DoughnutApplication in X.XXX seconds
Tomcat started on port(s): 8081 (http)
```

These should appear in:
- Startup script stdout/stderr (captured by Cloud Logging)
- Or in the application's own logs if properly configured

## Manual Verification Steps

1. **SSH into instance**:
   ```bash
   gcloud compute ssh <instance-id> --zone=us-east1-b
   ```

2. **Check if application is running**:
   ```bash
   ps aux | grep java
   curl http://localhost:8081/api/healthcheck
   ```

3. **Check startup script output**:
   ```bash
   journalctl -u google-startup-scripts.service --no-pager | grep -i "spring\|doughnut"
   ```

4. **Check if logs are being written**:
   ```bash
   # If using log file
   tail -f /var/log/doughnut-app.log
   
   # Or check process file descriptors
   lsof -p $(pgrep -f "doughnut.*jar")
   ```

## Next Steps If Still Not Working

1. **Verify the updated startup script is deployed**:
   - Check that instances are using the latest template
   - Verify startup script has the latest changes

2. **Check Cloud Logging agent configuration**:
   - The agent should automatically capture stdout/stderr
   - If not, may need explicit configuration

3. **Consider using systemd** (more reliable for production):
   - Create a systemd service unit
   - Ensures proper logging and process management

4. **Add explicit logging test**:
   - Add a log statement at application startup
   - Verify it appears in Cloud Logging

