#!/usr/bin/env bash
#
# The fixed commit-hook profiling corpus, sourced by profile-commit-hook.sh:
# 8 classes x 5 distinct staged changes, each to an input checked by the
# component scripts/quality_changed.sh selects for it.
#
# Compatible with macOS /bin/bash 3.2.

# One line per case: <case-id> <class> <path> [<path>]
corpus() {
  local java=backend/src/main/java/com/odde/donut
  cat <<EOF
frontend-1 frontend frontend/src/DonutApp.vue
frontend-2 frontend frontend/src/components/form/RichFrontmatterPropertyList.vue
frontend-3 frontend frontend/src/colors.ts
frontend-4 frontend frontend/src/composables/modalTopAnchor.ts
frontend-5 frontend frontend/src/utils/reservedReadmeTitles.ts
backend-1 backend ${java}/DonutApplication.java
backend-2 backend ${java}/configs/ScheduledJobErrorHandler.java
backend-3 backend ${java}/controllers/dto/AttachBookRequest.java
backend-4 backend ${java}/entities/EntityIdentifiedByIdOnly.java
backend-5 backend ${java}/services/McqService.java
cli-1 cli cli/src/backendApi/accessTokenStorage.ts
cli-2 cli cli/src/commands/notebook/notebookPublishAncestry.ts
cli-3 cli cli/src/commands/recall/recallMcqStageKeys.ts
cli-4 cli cli/src/inkAbsoluteContentPosition.ts
cli-5 cli cli/src/terminalColumns.ts
mcp-server-1 mcp-server mcp-server/src/context.ts
mcp-server-2 mcp-server mcp-server/src/helpers.ts
mcp-server-3 mcp-server mcp-server/src/server.ts
mcp-server-4 mcp-server mcp-server/src/tools/get-note-graph.ts
mcp-server-5 mcp-server mcp-server/src/types.ts
test-fixtures-1 test-fixtures packages/donut-test-fixtures/src/AnsweredQuestionBuilder.ts
test-fixtures-2 test-fixtures packages/donut-test-fixtures/src/ApiErrorBuilder.ts
test-fixtures-3 test-fixtures packages/donut-test-fixtures/src/BookFullBuilder.ts
test-fixtures-4 test-fixtures packages/donut-test-fixtures/src/Builder.ts
test-fixtures-5 test-fixtures packages/donut-test-fixtures/src/CircleBuilder.ts
root-1 root e2e_test/config/ci.ts
root-2 root e2e_test/step_definitions/ai.ts
root-3 root e2e_test/start/pageObjects/notePropertyLocationMethods.ts
root-4 root scripts/dev-restart.mjs
root-5 root scripts/generate-api-summary.mjs
openapi-1 openapi open_api_docs.yaml
openapi-2 openapi open_api_docs.yaml
openapi-3 openapi open_api_docs.yaml
openapi-4 openapi open_api_docs.yaml
openapi-5 openapi redocly.yaml
mixed-1 mixed frontend/src/composables/useRecallPageLoading.ts ${java}/controllers/dto/NotebookHealthFixRequest.java
mixed-2 mixed frontend/src/components/notes/widgets/NoteMoreOptionsForm.vue ${java}/entities/repositories/GlobalSettingRepository.java
mixed-3 mixed frontend/src/models/audio/rawSamples/rawSampleReceiver.ts ${java}/algorithms/ClozeReplacement.java
mixed-4 mixed frontend/src/components/svgs/relation_types/ObjectLeft.vue ${java}/exceptions/OpenAiNotAvailableException.java
mixed-5 mixed frontend/src/components/admin/questionGenerationBatchStatusText.ts ${java}/services/RecallAccuracyAggregator.java
EOF
}

# Valid, case-specific edit: a new export for TS/JS (changes the module's
# shape), a comment inside the Vue script block, a trailing comment otherwise.
change_file() {
  local file="$1" number="$2" marker="commit-hook profile case $2"
  case "$file" in
    *.ts|*.mjs)
      printf 'export const commitHookProfileCase%s = %s\n' "$number" "$number" >>"$file"
      ;;
    *.vue)
      awk -v line="// ${marker}" \
        '!done && /^<\/script>/ { print line; done = 1 } { print }' \
        "$file" >"$file.profile-tmp"
      mv "$file.profile-tmp" "$file"
      ;;
    *.java)
      printf '// %s\n' "$marker" >>"$file"
      ;;
    *.yaml|*.md)
      printf '# %s\n' "$marker" >>"$file"
      ;;
    *)
      echo "No corpus edit for $file" >&2
      exit 1
      ;;
  esac
}
