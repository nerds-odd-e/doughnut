<template>
  <div class="flex flex-col flex-1 min-w-0">
    <div class="flex w-full">
      <ProgressBar
        v-bind="{ title: `Recalling: `, finished, toRepeatCount, diligentMode }"
        @showSettings="showSettings = !showSettings"
      >
        <template #buttons>
          <div class="btn-group-wrapper relative" style="overflow: visible;">
            <div class="daisy-btn-group">
              <template v-if="previousAnsweredQuestionCursor !== undefined">
                <button
                  class="daisy-btn large-btn"
                  title="view previous answered question"
                  :disabled="finished === 0 || previousAnsweredQuestionCursor === 0"
                  @click="
                    $emit(
                      'viewLastAnsweredQuestion',
                      !previousAnsweredQuestionCursor
                        ? finished - 1
                        : previousAnsweredQuestionCursor! - 1
                    )
                  "
                >
                  <SkipBack class="w-8 h-8" />
                </button>
              </template>
              <button
                v-else
                class="daisy-btn large-btn"
                title="view last answered question"
                :disabled="finished === 0"
                @click="$emit('viewLastAnsweredQuestion', finished - 1)"
              >
                <Pause class="w-8 h-8 text-green-600" />
              </button>
            </div>
            <RecallSessionOptionsDialog
              v-if="showSettings"
              v-bind="{
                canMoveToEnd,
                previousAnsweredQuestionCursor,
                currentIndex,
                finished,
                toRepeatCount,
                totalAssimilatedCount,
                previousAnsweredQuestions,
              }"
              @close-dialog="showSettings = false"
              @move-to-end="handleMoveToEnd"
              @treadmill-mode-changed="$emit('treadmill-mode-changed')"
            />
          </div>
        </template>
        <template #cogIcon>
          <Settings class="w-6 h-6" />
        </template>
      </ProgressBar>
    </div>
    <div
      v-if="potentialLearningSessions.length > 0"
      class="flex flex-col gap-2 px-4"
    >
      <div
        v-for="session in potentialLearningSessions"
        :key="session.notebookId"
        data-test="potential-learning-session"
        role="status"
        class="text-base font-normal text-base-content"
      >
        {{ session.trackerIds.length }} potential learning session to commission for notebook "{{ session.notebookName }}"
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import ProgressBar from "../commons/ProgressBar.vue"
import { Pause, Settings, SkipBack } from "@lucide/vue"
import RecallSessionOptionsDialog from "./RecallSessionOptionsDialog.vue"

import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import type { PotentialLearningSession } from "@/composables/useRecallData"

defineProps({
  finished: { type: Number, required: true },
  toRepeatCount: { type: Number, required: true },
  previousAnsweredQuestionCursor: Number,
  canMoveToEnd: { type: Boolean, required: true },
  currentIndex: { type: Number, required: true },
  totalAssimilatedCount: { type: Number, default: 0 },
  diligentMode: { type: Boolean, default: false },
  previousAnsweredQuestions: {
    type: Array as () => (AnsweredQuestion | undefined)[],
    required: true,
  },
  potentialLearningSessions: {
    type: Array as () => PotentialLearningSession[],
    default: () => [],
  },
})

const emit = defineEmits<{
  (e: "viewLastAnsweredQuestion", cursor: number): void
  (e: "moveToEnd", index: number): void
  (e: "treadmill-mode-changed"): void
}>()

const showSettings = ref(false)

const handleMoveToEnd = (index: number) => {
  emit("moveToEnd", index)
}
</script>

<style lang="scss" scoped>
.btn-group-wrapper {
  display: flex;
  flex-direction: column;
}

.large-btn {
  padding: 0.75rem 1rem;
  min-height: 2.5rem;
  svg {
    width: 32px;
    height: 32px;
  }
  &:disabled {
    opacity: 0.5;
  }
}
</style>
