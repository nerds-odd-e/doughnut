<template>
  <AcceptRejectButtons
    :disabled="isProcessing"
    @accept="handleAccept"
    @cancel="handleCancel"
    @skip="handleSkip"
    :hideAccept="!canAccept"
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
import type { AnsweredQuestion } from "@generated/donut-backend-api"
import type { ToolCallResult } from "@/models/aiReplyState"
import AcceptRejectButtons from "@/components/commons/AcceptRejectButtons.vue"
import markdownizer from "../form/markdownizer"
import type { Suggestion } from "@/models/suggestions"
import { useToast } from "@/composables/useToast"
import { RecallPromptController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const { showErrorToast } = useToast()

const props = defineProps<{
  suggestion: Extract<Suggestion, { suggestionType: "exclusion" | "unknown" }>
  recallPromptId?: number
}>()

const emit = defineEmits<{
  (e: "resolved", result: ToolCallResult): void
  (e: "rejected", error: Error): void
  (e: "attempt-excluded", answeredQuestion: AnsweredQuestion): void
}>()

const isProcessing = ref(false)

const isExclusion = computed(
  () => props.suggestion.suggestionType === "exclusion"
)

const canAccept = computed(
  () =>
    props.suggestion.suggestionType === "exclusion" &&
    Boolean(props.suggestion.content.reason?.trim())
)

const suggestionTitle = computed(() => {
  switch (props.suggestion.suggestionType) {
    case "exclusion":
      return "Proposed correction: exclude this attempt as an ambiguous question"
    case "unknown":
      return `Unknown tool call: ${props.suggestion.content.functionName}`
  }
})

const suggestionContentClass = computed(() => {
  switch (props.suggestion.suggestionType) {
    case "exclusion":
      return "exclusion-reason"
    case "unknown":
      return "unknown-request"
  }
})

const formattedContent = computed(() => {
  switch (props.suggestion.suggestionType) {
    case "exclusion":
      return markdownizer.markdownToHtml(props.suggestion.content.reason)
    case "unknown":
      return `<pre>${props.suggestion.content.rawJson}</pre>`
  }
})

const handleAccept = async () => {
  if (props.suggestion.suggestionType !== "exclusion") return

  try {
    isProcessing.value = true
    const reason = props.suggestion.content.reason
    const { data, error } = await apiCallWithLoading(() =>
      RecallPromptController.excludeAsAmbiguous({
        path: { recallPrompt: props.recallPromptId! },
        body: { reason },
      })
    )
    if (error) throw new Error("Failed to exclude this attempt")
    emit("attempt-excluded", data!)
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
