<template>
  <div class="flex flex-col flex-1 min-w-0">
    <div class="flex w-full">
      <ProgressBar
        v-bind="{ title: `Recalling: `, finished, toRepeatCount, diligentMode }"
        @showSettings="showSettings = !showSettings"
      >
        <template #buttons>
          <div class="btn-group-wrapper relative" style="overflow: visible;">
            <div class="daisy-btn-group flex-nowrap items-center">
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
              <RecallLearningSessionActions
                :potential-learning-sessions="potentialLearningSessions"
                :awaiting-report-sessions="awaitingReportSessions"
                :recorded-sessions="recordedSessions"
                @select="
                  ({ mode, session }) => openSessionDialog(mode, session)
                "
              />
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
    <CommissionLearningSessionDialog
      v-if="sessionDialogProps"
      v-bind="sessionDialogProps"
      @close="sessionDialog = undefined"
      @commissioned="onSessionChanged"
      @recorded="onSessionChanged"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import ProgressBar from "../commons/ProgressBar.vue"
import { Pause, Settings, SkipBack } from "@lucide/vue"
import RecallSessionOptionsDialog from "./RecallSessionOptionsDialog.vue"
import CommissionLearningSessionDialog from "./CommissionLearningSessionDialog.vue"
import RecallLearningSessionActions from "./RecallLearningSessionActions.vue"
import type { LearningSessionActionMode } from "./LearningSessionListDialog.vue"
import { useRecallData } from "@/composables/useRecallData"

import type {
  AnsweredQuestion,
  LearningSessionLite,
} from "@generated/doughnut-backend-api"
import type { PotentialLearningSession } from "@/composables/useRecallData"

type SessionDialogState = {
  mode: LearningSessionActionMode
  session: PotentialLearningSession | LearningSessionLite
}

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
  awaitingReportSessions: {
    type: Array as () => LearningSessionLite[],
    default: () => [],
  },
  recordedSessions: {
    type: Array as () => LearningSessionLite[],
    default: () => [],
  },
})

const emit = defineEmits<{
  (e: "viewLastAnsweredQuestion", cursor: number): void
  (e: "moveToEnd", index: number): void
  (e: "treadmill-mode-changed"): void
}>()

const { requestDueRecallsRefresh } = useRecallData()
const showSettings = ref(false)
const sessionDialog = ref<SessionDialogState | undefined>(undefined)

const isLearningSessionLite = (
  session: PotentialLearningSession | LearningSessionLite
): session is LearningSessionLite => "learningSessionId" in session

const sessionDialogProps = computed(() => {
  const dialog = sessionDialog.value
  if (!dialog) return undefined
  const session = dialog.session
  const isLite = isLearningSessionLite(session)
  return {
    mode: dialog.mode === "commission" ? undefined : dialog.mode,
    notebookId: session.notebookId,
    notebookName: session.notebookName,
    learningSessionId:
      dialog.mode === "amend" && isLite ? session.learningSessionId : undefined,
    initialRequestMarkdown:
      dialog.mode !== "commission" && isLite
        ? session.requestMarkdown
        : undefined,
  }
})

const openSessionDialog = (
  mode: LearningSessionActionMode,
  session: PotentialLearningSession | LearningSessionLite
) => {
  sessionDialog.value = { mode, session }
}

const onSessionChanged = () => {
  requestDueRecallsRefresh()
}

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
