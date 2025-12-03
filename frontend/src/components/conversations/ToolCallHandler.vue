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
import type { NoteDetailsCompletion, Note } from "@generated/backend"
import type { ToolCallResult } from "@/models/aiReplyState"
import AcceptRejectButtons from "@/components/commons/AcceptRejectButtons.vue"
import markdownizer from "../form/markdownizer"
import type { Suggestion } from "@/models/suggestions"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()

const props = defineProps<{
  suggestion: Suggestion
  note?: Note
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
  if (!completion.patch) return ""

  // Display the patch in a readable format
  // Escape markdown special characters in the patch for display
  const escapedPatch = completion.patch
    .replace(/\n/g, "\n")
    .replace(/```/g, "\\`\\`\\`")

  return `\`\`\`diff\n${escapedPatch}\n\`\`\``
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
  if (!props.suggestion || !props.note) return

  try {
    isProcessing.value = true
    switch (props.suggestion.suggestionType) {
      case "completion": {
        const content = props.suggestion.content
        await storageAccessor.value
          .storedApi()
          .completeDetails(props.note.id, content)
        break
      }
      case "title": {
        const content = props.suggestion.content
        await storageAccessor.value
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
