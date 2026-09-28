# Keep production secrets out of the app process command line

## Source

- Story: [Keep production secrets out of the app process command line](../../seeds/SEED-008-prod-secrets-visible-in-process-args.md#story-1)
- **Identity:** SEED-008#story-1

## Goal and scope

The production app starts without the database password, GitHub issues token,
or OpenAI key on its `java` command line, so listing processes or reading a JVM
crash file on an app instance no longer shows them. The app keeps working on
all three secrets.

Excluded (see the seed): rotating any credential, switching pinned Secret
Manager versions to `latest`, removing the unused
`GITHUB_DOUGHNUT_REPO_ACCESS_TOKEN` fetch, secret files or mounts, hiding
environment variables, non-root user, SSH/IAM tightening, heap dumps, and any
committed test that the flags are absent.

## Approach

Delete the three secret `-D` flags from the `java` command in
`infra/gcp/scripts/mig-zulu25-openai-app-instance-startup.sh` (lines 149–151).
`application-prod.yml` already resolves the same properties from the
environment variables the script exports, and the `bash -c` child inherits
them. Leave every other flag unchanged.

## Decisive premises

| Premise | Observation | Result |
| --- | --- | --- |
| The prod profile already reads the three secrets from environment variables | `backend/src/main/resources/application-prod.yml` | `password: ${MYSQL_PASSWORD}`, `token: ${GITHUB_FOR_ISSUES_API_TOKEN}`, `token: ${OPENAI_API_TOKEN}` |
| The startup script exports those variables before starting `java` | `mig-zulu25-openai-app-instance-startup.sh:74,102,109` | All three are `export`ed; `java` runs in a `bash -c` child that inherits them |
| Environment-only secrets already work in production | `application-prod.yml:61-62` reads `${OAUTH2_github_client_id}` / `${OAUTH2_github_client_secret}` with no default and no `-D` flag; an unresolvable placeholder fails startup | Production starts, so the same env-var path works there |
| Only these three lines put secrets on a command line | `grep -rn -- "-Dspring\.[a-z_.]*\(password\|token\|secret\)" infra scripts .github` | Only lines 149–151 of the startup script |
| No test depends on the flags | `grep -rn "datasource.password\|openai.token\|github_for_issues.token" scripts/test e2e_test` | No matches |
| The script tests that exercise the startup script and deploy run green on trunk | `bash scripts/test/production-database-routing.test`; `bash scripts/test/deploy-backend-jar-to-gcp-mig.sh.test` | Both pass |
| A changed startup script reaches production through the normal release | `deploy-backend-jar-to-gcp-mig.sh` compares the startup script's SHA-256 to the last deploy record and runs `update-mig-startup-script.sh` (template + rolling replace); covered by `deploy-backend-jar-to-gcp-mig.sh.test`; called from `publish-application.sh` in `.github/workflows/deploy.yml` | Holds, but only on an authorized application release (immutable tag), not on a push to `main` |
| A started app proves the database password | `application-prod.yml` enables Flyway, which migrates at startup; `/api/healthcheck` itself does not query the database | Healthy after rollout ⇒ database authentication worked |

## Slices

### 1. The production app starts without secrets on its command line
Type: Behavior
Status: delivered; production check pending the next application release
Accepted local proof: `CURSOR_DEV=true nix develop -c bash scripts/test/run_all_script_tests.sh`
→ 20/20 pass, including `production-database-routing.test` (runs the startup
script with fakes) and `mig-startup-java-command.test`. The three removed flags
set exactly `spring.datasource.password`, `spring.github_for_issues.token`, and
`spring.openai.token`, which `application-prod.yml` reads from the variables the
script still exports.
Proof:
- Local: `bash scripts/test/run_all_script_tests.sh` stays green (the routing
  test runs the whole startup script with fakes).
- Production, after the next owner-authorized application release (external
  wait; owner holds the release tag and `gcloud` login). For each app instance
  (`gcloud compute instances list`), count secret flags without printing them:
  `gcloud compute ssh <instance> --zone=us-east1-b --command='for p in $(pgrep -f donut-0.0.1-SNAPSHOT.jar); do tr "\0" "\n" </proc/$p/cmdline; done | awk "/spring.profiles.active/{p++} /datasource.password|openai.token|github_for_issues.token/{s++} END{print \"profile flags:\", p+0, \"secret flags:\", s+0}"'`
  prints `profile flags:` ≥ 1 (proves the command line was read) and
  `secret flags: 0`. The loop covers the `bash -c` parent as well as `java`;
  `/proc/<pid>/cmdline` needs no `sudo`. Never run `ps aux` for this check; it would print the secrets
  again if the change had not landed.
- Production still works: the release's healthcheck step passes (startup
  migrated the database), a note page loads, and one AI-backed action returns a
  result.

Behavior: the startup script launches the app → the Java process's command line
carries the profile, datasource URL, logging and JVM flags but no password or
token → the app starts and uses all three secrets as before.

## Current decisions

- No credential rotation in this story (owner, during refinement: no evidence
  the August output left the owner's control).
- The production check is a one-time demonstration, not a committed test
  (removed things leave no negative tests).
- Execution may deliver the code change to `main` and stop with the
  production check pending the next release; the story is not complete until
  that check is recorded here.

## Execution complete

Product advice: no backlog change. The only remaining obligation is this plan's
one-time production check after the next owner-authorized application release;
record its result here before story wrap-up. Owner exclusions (no rotation, keep
the unused repo-access token fetch) stand.
