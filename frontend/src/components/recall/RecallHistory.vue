<script setup lang="ts">
import type { RecallHistoryItem } from "@generated/donut-backend-api"
import QuestionDisplay from "@/components/recall/QuestionDisplay.vue"
import ConversationButton from "@/components/recall/ConversationButton.vue"

defineProps<{
  items: RecallHistoryItem[]
}>()

const formatThinkingTime = (ms: number): string => {
  if (ms < 1000) {
    return `${ms}ms`
  }
  const seconds = ms / 1000
  if (seconds < 60) {
    return `${seconds.toFixed(1)}s`
  }
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = Math.floor(seconds % 60)
  return `${minutes}m ${remainingSeconds}s`
}

const historyItemKey = (item: RecallHistoryItem, index: number) => {
  if (item.recallLog) {
    return `log-${item.recallLog.id}`
  }
  if (item.recallPrompt) {
    return `prompt-${item.recallPrompt.id}`
  }
  return `empty-${index}`
}
</script>

<template>
  <div
    v-if="items.length === 0"
    class="daisy-alert daisy-alert-info"
  >
    No recall history found for this memory tracker.
  </div>
  <div v-else data-testid="recall-history">
    <div
      v-for="(item, index) in items"
      :key="historyItemKey(item, index)"
      class="daisy-card shadow-sm mb-4"
      data-testid="recall-history-item"
    >
      <div class="daisy-card-body">
        <div
          v-if="item.recallLog"
          data-testid="recall-log"
          class="text-sm flex flex-wrap gap-4 mb-2"
        >
          <span data-testid="recall-log-product-outcome">{{
            item.recallLog.productOutcome
          }}</span>
          <span>Recorded: {{ new Date(item.recallLog.recordedAt).toLocaleString() }}</span>
          <span>Elapsed hours: {{ item.recallLog.elapsedHours }}</span>
        </div>
        <p
          v-if="item.recallLog?.tutorFeedback"
          data-testid="recall-log-tutor-feedback"
          class="text-sm mb-2 whitespace-pre-wrap"
        >
          {{ item.recallLog.tutorFeedback }}
        </p>
        <template v-if="item.recallPrompt">
          <div class="text-sm text-base-content/70 mb-2 flex gap-2 flex-wrap">
            <span v-if="item.recallPrompt.questionGeneratedTime">
              Generated:
              {{ new Date(item.recallPrompt.questionGeneratedTime).toLocaleString() }}
            </span>
            <span
              v-if="item.recallPrompt.isContested"
              class="daisy-badge daisy-badge-warning"
            >
              Contested
            </span>
            <span v-if="item.recallPrompt.answerTime">
              Answered:
              {{ new Date(item.recallPrompt.answerTime).toLocaleString() }}
            </span>
            <span v-else>Unanswered</span>
            <span v-if="item.recallPrompt.answer?.thinkingTimeMs">
              Thinking time:
              {{ formatThinkingTime(item.recallPrompt.answer.thinkingTimeMs) }}
            </span>
            <span
              v-if="item.recallPrompt.answer?.awayMs"
              data-testid="recall-history-away-time"
            >
              Away: {{ formatThinkingTime(item.recallPrompt.answer.awayMs) }}
              ({{ item.recallPrompt.answer.awayCount }}x)
            </span>
            <span
              v-if="item.recallPrompt.answer?.detourMs"
              data-testid="recall-history-detour-time"
              class="daisy-badge daisy-badge-outline"
            >
              Detour: {{ formatThinkingTime(item.recallPrompt.answer.detourMs) }}
              ({{ item.recallPrompt.answer.detourCount }}x)
            </span>
            <span
              v-if="item.recallPrompt.answer?.idleMs"
              data-testid="recall-history-idle-time"
              class="daisy-badge daisy-badge-warning"
            >
              Idle: {{ formatThinkingTime(item.recallPrompt.answer.idleMs) }}
            </span>
          </div>
          <div v-if="item.recallPrompt.questionType === 'SPELLING'">
            <div v-if="item.recallPrompt.answer" class="space-y-2">
              <div class="flex items-center gap-2">
                <span class="font-semibold">Your answer:</span>
                <span>{{ item.recallPrompt.answer.spellingAnswer }}</span>
              </div>
              <div
                v-if="item.recallPrompt.answer.correct != null"
                class="flex items-center gap-2"
              >
                <span class="font-semibold">Result:</span>
                <span
                  :class="{
                    'daisy-badge-success': item.recallPrompt.answer.correct,
                    'daisy-badge-error': !item.recallPrompt.answer.correct,
                  }"
                  class="daisy-badge"
                >
                  {{ item.recallPrompt.answer.correct ? "Correct" : "Incorrect" }}
                </span>
              </div>
            </div>
            <div v-else class="daisy-alert daisy-alert-info">
              This is a spelling question. Details are not needed.
            </div>
          </div>
          <QuestionDisplay
            v-else-if="item.recallPrompt.mcq && item.recallPrompt.answer"
            v-bind="{
              questionStem: item.recallPrompt.mcq.questionStem,
              responseChoices: item.recallPrompt.mcq.responseChoices,
              correctChoiceIndex: item.recallPrompt.mcq.correctAnswerIndex,
              answer: item.recallPrompt.answer,
              disabled: true,
              testedFocus: item.recallPrompt.mcq.testedFocus,
              validationRationale: item.recallPrompt.mcq.validationRationale,
            }"
          />
          <QuestionDisplay
            v-else-if="item.recallPrompt.mcq"
            v-bind="{
              questionStem: item.recallPrompt.mcq.questionStem,
              responseChoices: item.recallPrompt.mcq.responseChoices,
              disabled: true,
            }"
          />
          <ConversationButton
            v-if="item.recallPrompt.answer"
            :recall-prompt-id="item.recallPrompt.id"
          />
        </template>
      </div>
    </div>
  </div>
</template>
