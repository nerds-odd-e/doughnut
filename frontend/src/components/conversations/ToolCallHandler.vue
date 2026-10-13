<template>
  <AcceptRejectButtons
    :disabled="isProcessing"
    @accept="handleAccept"
    @cancel="handleCancel"
    @skip="handleSkip"
    :hideAccept="suggestion.suggestionType === 'unknown'"
    :hideSkip="isExclusion"
    :acceptLabel="isExclusion ? 'Confirm' : undefined"
    :cancelLabel="isExclusion ? 'Decline' : undefined"
  >
    <template #title>
      {{ suggestionTitle }}
    </template>
    <template #content>
      <div :class="suggestionContentClass" v-html="formattedContent" />
    </template>
  </AcceptRejectButtons>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import type { NoteContentCompletion, Note } from "@generated/donut-backend-api"
import type { ToolCallResult } from "@/models/aiReplyState"
import AcceptRejectButtons from "@/components/commons/AcceptRejectButtons.vue"
import markdownizer from "../form/markdownizer"
import { RICH_CONTENT_PROSE } from "@/constants/richContentProse"
import type { Suggestion } from "@/models/suggestions"
import { useNoteStore } from "@/store/noteStore"
import { useToast } from "@/composables/useToast"
import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const noteStore = useNoteStore()
const { showErrorToast } = useToast()

const props = defineProps<{
  suggestion: Suggestion
  note?: Note
  recallPromptId?: number
}>()

const emit = defineEmits<{
  (e: "resolved", result: ToolCallResult): void
  (e: "rejected", error: Error): void
}>()

const isProcessing = ref(false)

const isExclusion = computed(
  () => props.suggestion.suggestionType === "exclusion"
)

const suggestionTitle = computed(() => {
  switch (props.suggestion?.suggestionType) {
    case "completion":
      return "Suggested completion:"
    case "title":
      return "Suggested title:"
    case "exclusion":
      return "Proposed correction: exclude this attempt as an ambiguous question"
    case "unknown":
      return `Unknown tool call: ${
        // biome-ignore lint/suspicious/noExplicitAny: Type is dynamically determined at runtime
        (props.suggestion.content as any).functionName
      }`
    default:
      return ""
  }
})

const suggestionContentClass = computed(() => {
  switch (props.suggestion?.suggestionType) {
    case "completion":
      return `completion-text ${RICH_CONTENT_PROSE}`
    case "title":
      return "title-suggestion"
    case "exclusion":
      return "exclusion-reason"
    case "unknown":
      return "unknown-request"
    default:
      return ""
  }
})

const formatCompletionSuggestion = (completion: NoteContentCompletion) => {
  if (!completion.content) return ""

  return completion.content
}

const formattedContent = computed(() => {
  if (!props.suggestion) return ""

  switch (props.suggestion.suggestionType) {
    case "completion":
      return markdownizer.markdownToHtml(
        formatCompletionSuggestion(props.suggestion.content)
      )
    case "title":
      return props.suggestion.content
    case "exclusion":
      return markdownizer.markdownToHtml(props.suggestion.content.reason)
    case "unknown":
      return `<pre>${(props.suggestion.content as { rawJson: string }).rawJson}</pre>`
    default:
      return ""
  }
})

const handleAccept = async () => {
  if (!props.suggestion || (!isExclusion.value && !props.note)) return

  try {
    isProcessing.value = true
    switch (props.suggestion.suggestionType) {
      case "completion": {
        const content = props.suggestion.content
        await noteStore.completeContent(props.note!.id, content)
        break
      }
      case "title": {
        const content = props.suggestion.content
        await noteStore.updateTextField(props.note!.id, "edit title", content)
        break
      }
      case "exclusion": {
        const reason = props.suggestion.content.reason
        const { error } = await apiCallWithLoading(() =>
          RecallPromptController.excludeAsAmbiguous({
            path: { recallPrompt: props.recallPromptId! },
            body: { reason },
          })
        )
        if (error) throw new Error("Failed to exclude this attempt")
        break
      }
    }
    emit("resolved", { status: "accepted" })
  } catch (error) {
    const errorMessage =
      error instanceof Error ? error.message : "Failed to apply patch"
    showErrorToast(errorMessage)
    emit("rejected", error as Error)
  } finally {
    isProcessing.value = false
  }
}

const handleCancel = () => {
  emit("rejected", new Error("Tool call was rejected"))
}

const handleSkip = () => {
  emit("resolved", { status: "skipped" })
}
</script>

<style scoped>
.completion-text {
  font-style: italic;
  color: #666;
}

.title-suggestion {
  font-style: italic;
  color: #666;
  font-weight: bold;
}

.unknown-request {
  font-family: monospace;
  background-color: #f8f9fa;
  padding: 0.5rem;
  border-radius: 0.25rem;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.unknown-request :deep(pre) {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
}
</style>
