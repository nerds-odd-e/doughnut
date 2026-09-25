# Reading production application logs

## Where the log is

The `prod` profile in `backend/src/main/resources/logback-spring.xml` writes
the application log to `logs/donut-prod.log` under the Java process's working
directory. The MIG startup script starts Java from `/`, so on an instance the
log is `/logs/donut-prod.log`, rolled into `/logs/archived/donut-prod.1.log`
to `.3.log` at 5 MB each.

The application log is **not** in Cloud Logging: no logging agent is installed
on the instances. There is no `/var/log/doughnut-app.log`.

## Read it

```bash
# Instance names: gcloud compute instances list
./infra/gcp/scripts/check-app-logs-on-instance.sh <instance-name>
```

The script prints the application's health, the end of the log, and the last
50 `ERROR` lines across the log and its archives, including startup errors.

## The log lives only as long as the instance's disk

A deploy (rolling replace), an autohealing repair, or any other recreate gives
the instance a new boot disk, and its log is gone. Read a release's startup
output before the next deploy, and before resetting an unhealthy instance:
autohealing may recreate it.

## When the application does not start

The serial console shows boot and startup-script output:

```bash
./infra/gcp/scripts/view-mig-doughnut-app-instance-logs.sh <instance-name>
./infra/gcp/scripts/tail-mig-doughnut-app-instance-logs.sh <instance-name>
```
