---
id: SEED-008
status: dormant
planted: 2026-08-28
planted_during: recall-split-half-reliability prod deploy investigation
trigger_when: when touching the MIG instance startup script, prod secret rotation, or any security review of the GCP app instances
scope: small
---

# SEED-008: Production secrets are visible in plaintext via `ps aux` on the app instance

## Why This Matters

`infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` launches the Spring Boot app with the DB password, GitHub token, and OpenAI API key passed as `-D` JVM system properties on the `java` command line (`-Dspring.datasource.password=${MYSQL_PASSWORD}`, `-Dspring.github_for_issues.token=${GITHUB_FOR_ISSUES_API_TOKEN}`, `-Dspring.openai.token=${OPENAI_API_TOKEN}`). Command-line arguments are visible to any user able to run `ps aux` (or read `/proc/<pid>/cmdline`) on the instance, and JVM crash files record the full command line too. This was observed while diagnosing an unrelated deploy issue, where a routine `ps aux | grep java` printed all three secrets in plaintext.

The flags are redundant: `backend/src/main/resources/application-prod.yml` already reads the same three values from the environment variables the startup script exports (`${MYSQL_PASSWORD}`, `${GITHUB_FOR_ISSUES_API_TOKEN}`, `${OPENAI_API_TOKEN}`), exactly as it already does for the GitHub OAuth client secret.

Only people who can SSH into the instance can list its processes, and they can already read Secret Manager through the instance's service account. So this is not a boundary against a deliberate insider; the real risk is accidental disclosure when diagnostic output is copied into terminals, chats, AI transcripts, or screen shares.

## When to Surface

**Trigger:** touching `mig-zulu25-openai-app-instance-startup.sh`, rotating the DB/GitHub/OpenAI credentials, or doing a security review of the prod GCP instances.

## Scope Estimate

**Small** — delete the three redundant secret `-D` flags from the startup script; the normal deploy replaces the instances.

## Story Decomposition

<a id="story-1"></a>

### Keep production secrets out of the app process command line

**Identity:** SEED-008#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/004-secrets-off-command-line/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"90a04234d3842f3d646e6b4ec330f0963c8789aaceb5523fd2c586f141590afc","plan":"d79ccf46752a2d79338c03c60bf92e3a6100c94a00acc9dd15094c384d7a4b37"}}
```

**Goal**

People diagnosing a production app instance no longer see the database
password, GitHub issues token, or OpenAI key when they list processes or read a
JVM crash file. This hardens production against accidental disclosure of
secrets through routine diagnostic output that gets copied elsewhere.

**Scope**

Decided by the owner during refinement: keep only what is a verified, real risk
and can be checked.

Promised:

1. The production app starts without any secret on its command line. The
   database password, GitHub issues token, and OpenAI key reach the app only
   through the environment variables `application-prod.yml` already reads.
2. Production keeps working on those three secrets: the app is healthy (it
   connects to the database), AI features respond (OpenAI key), and the GitHub
   issue path keeps working.
3. The change reaches production through the normal release deploy, which
   updates the instance template when the startup script changes.

Not promised by this story:

- Rotating any credential. There is no evidence the August output left the
  owner's control, and rotation cannot be verified as closing a leak. Rotation
  stays a separate owner decision; the OpenAI key is the cheapest and most
  valuable to rotate if that is ever decided.
- Switching the pinned Secret Manager versions (`versions/1`) to `latest`;
  it only matters for rotation.
- Removing the unused `GITHUB_DOUGHNUT_REPO_ACCESS_TOKEN` fetch; it sits in the
  environment, not the command line.
- Secret files or Secret Manager mounts, hiding environment variables (only
  root can read `/proc/<pid>/environ`, and root can read Secret Manager anyway),
  running the app as a non-root user, and SSH or IAM tightening.
- Heap dumps, which hold secrets in memory however they are passed in.
- A committed test that the flags are absent; removed things leave no negative
  tests behind.

**Key examples**

- After the release deploy, on a production app instance, counting secret-like
  flags in the Java process's command line (without printing it) gives 0.
  Before, `ps aux | grep java` printed all three secrets.
- After the same deploy, the load balancer health check passes, a note page
  loads data from the database, and an AI-backed action returns a result.
- The non-secret flags (active profile, datasource URL, logging levels, JVM
  tuning) stay on the command line unchanged.

**Effort hypothesis:** XS, high confidence. A three-line deletion plus one
deploy and a one-time production check.

## Breadcrumbs

- `infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` — the `java ${JAVA_OPTS} ... -jar` command with secrets as `-D` args
- `backend/src/main/resources/application-prod.yml` — already reads the three secrets from environment variables
- `infra/gcp/scripts/deploy-backend-jar-to-gcp-mig.sh` — updates the MIG template when the startup script's hash changes
- `docs/secrets_management.md` — existing secrets doc; doesn't cover this vector

## Notes

Captured during the investigation into why `/api/user/recall-split-half-reliability` 405'd in production (root cause turned out to be an unrelated stale-`ARTIFACT`-name bug in the same startup script, fixed directly). The secrets exposure was noticed as a side effect of running the then-documented `ps aux` diagnostic and was never itself the thing being investigated.
