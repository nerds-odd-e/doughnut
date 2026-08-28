# Deployed commit verification

**Status:** done.

`GET /api/healthcheck` reports `OK. Active Profile: <profiles>. Commit: <sha>`
from Gradle `bootBuildInfo` (`build.commit` = `git rev-parse HEAD`; `time`
excluded for jar reproducibility). After MIG rollout,
`app-instance-healthcheck.sh` retries until that commit equals `$GITHUB_SHA`,
then fails the Deploy job (existing Slack `Notify-on-failure`) on mismatch.

Prod secrets in process args remain at
`.planning/seeds/SEED-008-prod-secrets-visible-in-process-args.md`.
