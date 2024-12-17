<template>
  <AcceptRejectButtons
    :disabled="isProcessing"
    @accept="handleAccept"
    @cancel="handleCancel"
    @skip="handleSkip"
    :hideAccept="suggestion.suggestionType === 'unknown'"
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
import type {
  NoteDetailsCompletion,
  Note,
  ToolCallResult,
} from "@/generated/backend"
import AcceptRejectButtons from "@/components/commons/AcceptRejectButtons.vue"
import markdownizer from "../form/markdownizer"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { Suggestion } from "@/models/suggestions"

const props = defineProps<{
  suggestion: Suggestion
  note?: Note
  storageAccessor?: StorageAccessor
}>()

const emit = defineEmits<{
  (e: "resolved", result: ToolCallResult): void
  (e: "rejected", error: Error): void
}>()

const isProcessing = ref(false)

const suggestionTitle = computed(() => {
  switch (props.suggestion?.suggestionType) {
    case "completion":
      return "Suggested completion:"
    case "title":
      return "Suggested title:"
    case "unknown":
      return `Unknown tool call: ${
        // biome-ignore lint/suspicious/noExplicitAny: <explanation>
        (props.suggestion.content as any).functionName
      }`
    default:
      return ""
  }
})

const suggestionContentClass = computed(() => {
  switch (props.suggestion?.suggestionType) {
    case "completion":
      return "completion-text"
    case "title":
      return "title-suggestion"
    case "unknown":
      return "unknown-request"
    default:
      return ""
  }
})

const formatCompletionSuggestion = (completion: NoteDetailsCompletion) => {
  const currentDetails = props.note?.details?.trim() || ""
  if (!currentDetails) return completion.completion

  if (!completion.deleteFromEnd) return `...${completion.completion}`

  const textToDelete =
    currentDetails.slice(-completion.deleteFromEnd) || currentDetails
  const strikeThroughText = textToDelete.replace(/ /g, "·").replace(/\n/g, "↵")
  return `~~${strikeThroughText}~~${completion.completion}`
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
    case "unknown":
      return `<pre>${(props.suggestion.content as { rawJson: string }).rawJson}</pre>`
    default:
      return ""
  }
})

const handleAccept = async () => {
  if (!props.suggestion || !props.note || !props.storageAccessor) return

  try {
    isProcessing.value = true
    switch (props.suggestion.suggestionType) {
      case "completion": {
        const content = props.suggestion.content
        await props.storageAccessor
          .storedApi()
          .completeDetails(props.note.id, content)
        break
      }
      case "title": {
        const content = props.suggestion.content
        await props.storageAccessor
          .storedApi()
          .updateTextField(props.note.id, "edit title", content)
        break
      }
    }
    emit("resolved", { status: "accepted" })
  } catch (error) {
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
