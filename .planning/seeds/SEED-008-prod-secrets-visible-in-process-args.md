---
id: SEED-008
status: dormant
planted: 2026-08-28
planted_during: recall-split-half-reliability prod deploy investigation
trigger_when: when touching the MIG instance startup script, prod secret rotation, or any security review of the GCP app instances
scope: medium
---

# SEED-008: Production secrets are visible in plaintext via `ps aux` on the app instance

## Why This Matters

`infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` launches the Spring Boot app with the DB password, GitHub token, and OpenAI API key passed as `-D` JVM system properties on the `java` command line (lines ~96-105: `-Dspring.datasource.password=${MYSQL_PASSWORD}`, `-Dspring.github_for_issues.token=${GITHUB_FOR_ISSUES_API_TOKEN}`, `-Dspring.openai.token=${OPENAI_API_TOKEN}`). Command-line arguments are visible to any user able to run `ps aux` (or read `/proc/<pid>/cmdline`) on the instance — this was observed directly while diagnosing an unrelated deploy issue, where a routine `ps aux | grep java` for log-debugging purposes printed all three secrets in plaintext.

The existing troubleshooting docs (`docs/gcp/troubleshooting-springboot-logs.md`, `docs/gcp/finding-springboot-logs-in-cloud-logging.md`) instruct running `ps aux | grep java` as a normal diagnostic step, so this exposure is trivially hit by anyone following the documented playbook, not just an attacker. `docs/secrets_management.md` only covers git-secret/GnuPG for encrypting secret files in the repo and does not address runtime process-argument exposure.

## When to Surface

**Trigger:** touching `mig-zulu25-openai-app-instance-startup.sh`, rotating the DB/GitHub/OpenAI credentials, or doing a security review of the prod GCP instances.

## Scope Estimate

**Medium** — pass these three secrets as environment variables (not visible in `ps aux`) instead of `-D` args, or mount them from Secret Manager into a file Spring reads at startup. Requires updating the startup script and confirming Spring Boot picks up the equivalent `SPRING_DATASOURCE_PASSWORD` / etc. env vars correctly, then rotating the three credentials since they've been exposed.

## Breadcrumbs

- `infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` — the `java ${JAVA_OPTS} ... -jar` command with secrets as `-D` args
- `docs/gcp/troubleshooting-springboot-logs.md`, `docs/gcp/finding-springboot-logs-in-cloud-logging.md` — recommend `ps aux | grep java`, which surfaces the leak
- `docs/secrets_management.md` — existing secrets doc; doesn't cover this vector

## Notes

Captured during the investigation into why `/api/user/recall-split-half-reliability` 405'd in production (root cause turned out to be an unrelated stale-`ARTIFACT`-name bug in the same startup script, fixed directly). The secrets exposure was noticed as a side effect of running the documented `ps aux` diagnostic and was never itself the thing being investigated.
