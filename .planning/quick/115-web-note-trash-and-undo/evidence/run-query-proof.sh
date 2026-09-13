#!/usr/bin/env bash
set -euo pipefail
trash_evidence_dir="$(cd "$(dirname "$0")" && pwd)"
trash_repo_dir="$(cd "$trash_evidence_dir/../../../.." && pwd)"
trash_probe_dir="$(mktemp -d /tmp/dough-trash-query-proof.XXXXXX)"
export TRASH_PROBE_CLASSPATH="$trash_probe_dir/classpath.txt"
cat > "$trash_probe_dir/classpath.gradle" <<'GRADLE'
allprojects {
  afterEvaluate {
    if (plugins.hasPlugin('java')) {
      tasks.register('trashPlanningClasspath') {
        doLast {
          new File(System.getenv('TRASH_PROBE_CLASSPATH')).text = configurations.testRuntimeClasspath.asPath
        }
      }
    }
  }
}
GRADLE
"$trash_repo_dir/backend/gradlew" -p "$trash_repo_dir/backend" -I "$trash_probe_dir/classpath.gradle" trashPlanningClasspath --quiet
java --class-path "$(cat "$TRASH_PROBE_CLASSPATH")" "$trash_evidence_dir/TrashQueryProof.java"
