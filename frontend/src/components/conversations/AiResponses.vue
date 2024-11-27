<template>
  <div>
    <div v-if="currentAiReply" class="d-flex mb-3">
      <div class="message-avatar me-2" title="AI Assistant">
        <SvgRobot />
      </div>
      <div class="card py-2 px-3 bg-light ai-chat" v-html="markdowntToHtml(currentAiReply)" />
    </div>

    <div v-if="completionSuggestion" class="d-flex mb-3">
      <div class="message-avatar me-2" title="AI Assistant">
        <SvgRobot />
      </div>
      <AcceptRejectButtons
        :disabled="isProcessingToolCall"
        @accept="$emit('accept-completion')"
        @cancel="$emit('cancel')"
        @skip="$emit('skip')"
      >
        <template #title>Suggested completion:</template>
        <template #content>
          <div class="completion-text" v-html="markdowntToHtml(formattedCompletionSuggestion)" />
        </template>
      </AcceptRejectButtons>
    </div>

    <div v-if="aiStatus" class="d-flex align-items-center status-bar mb-3">
      <div class="spinner-border spinner-border-sm me-2" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <small class="text-secondary">{{ aiStatus }}</small>
    </div>

    <div v-if="topicTitleSuggestion" class="d-flex mb-3">
      <div class="message-avatar me-2" title="AI Assistant">
        <SvgRobot />
      </div>
      <AcceptRejectButtons
        :disabled="isProcessingToolCall"
        @accept="$emit('accept-title')"
        @cancel="$emit('cancel')"
        @skip="$emit('skip')"
      >
        <template #title>Suggested title:</template>
        <template #content>
          <div class="title-suggestion">{{ topicTitleSuggestion }}</div>
        </template>
      </AcceptRejectButtons>
    </div>

    <div v-if="unknownRequestSuggestion" class="d-flex mb-3">
      <div class="message-avatar me-2" title="AI Assistant">
        <SvgRobot />
      </div>
      <AcceptRejectButtons
        :disabled="isProcessingToolCall"
        @cancel="$emit('cancel')"
        @skip="$emit('skip')"
        :hideAccept="true"
      >
        <template #title>Unknown tool call: {{ unknownRequestSuggestion.functionName }}</template>
        <template #content>
          <div class="unknown-request">
            <pre>{{ unknownRequestSuggestion.rawJson }}</pre>
          </div>
        </template>
      </AcceptRejectButtons>
    </div>

    <div v-if="lastErrorMessage" class="last-error-message text-danger mb-3">
      {{ lastErrorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import AcceptRejectButtons from "@/components/commons/AcceptRejectButtons.vue"
import markdownizer from "../form/markdownizer"

const props = defineProps<{
  currentAiReply?: string
  completionSuggestion?: string
  aiStatus?: string
  topicTitleSuggestion?: string
  unknownRequestSuggestion?: {
    rawJson: string
    functionName: string
  }
  lastErrorMessage?: string
  isProcessingToolCall: boolean
  currentDetails?: string
}>()

defineEmits<{
  (e: "accept-completion"): void
  (e: "accept-title"): void
  (e: "cancel"): void
  (e: "skip"): void
}>()

const markdowntToHtml = (content?: string) =>
  markdownizer.markdownToHtml(content)

const formattedCompletionSuggestion = computed(() => {
  if (!props.completionSuggestion) return ""
  return props.currentDetails
    ? `...${props.completionSuggestion}`
    : props.completionSuggestion
})
</script>

