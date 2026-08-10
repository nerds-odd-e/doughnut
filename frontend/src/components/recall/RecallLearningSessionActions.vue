<template>
  <button
    type="button"
    class="daisy-btn large-btn learning-session-actions-btn"
    title="Learning session actions"
    data-test="learning-session-actions"
    @click="showListDialog = true"
  >
    <GraduationCap class="w-8 h-8" />
    <div
      v-if="actionableSessionCount > 0"
      class="learning-session-actions-badge"
      data-test="learning-session-actions-badge"
    >
      {{ actionableSessionCount }}
    </div>
  </button>
  <LearningSessionListDialog
    v-if="showListDialog"
    :entries="actionableSessions"
    @close="showListDialog = false"
    @select="onListEntrySelect"
  />
</template>

<script setup lang="ts">
import { computed, ref } from "vue"
import { GraduationCap } from "@lucide/vue"
import type { LearningSessionLite } from "@generated/doughnut-backend-api"
import type { PotentialLearningSession } from "@/composables/useRecallData"
import LearningSessionListDialog, {
  type ActionableSessionEntry,
  type LearningSessionActionMode,
} from "./LearningSessionListDialog.vue"

const props = defineProps<{
  potentialLearningSessions: PotentialLearningSession[]
  awaitingReportSessions: LearningSessionLite[]
  recordedSessions: LearningSessionLite[]
}>()

const emit = defineEmits<{
  (
    e: "select",
    payload: {
      mode: LearningSessionActionMode
      session: PotentialLearningSession | LearningSessionLite
    }
  ): void
}>()

const showListDialog = ref(false)

const actionableSessions = computed((): ActionableSessionEntry[] => {
  const entries: ActionableSessionEntry[] = []
  for (const session of props.potentialLearningSessions) {
    entries.push({
      key: `potential-${session.notebookId}`,
      mode: "request",
      notebookName: session.notebookName,
      session,
      actionLabel: "Commission",
    })
  }
  for (const session of props.awaitingReportSessions) {
    entries.push({
      key: `awaiting-${session.learningSessionId}`,
      mode: "record",
      notebookName: session.notebookName,
      session,
      actionLabel: "Record report",
    })
  }
  for (const session of props.recordedSessions) {
    entries.push({
      key: `recorded-${session.learningSessionId}`,
      mode: "amend",
      notebookName: session.notebookName,
      session,
      actionLabel: "Amend report",
    })
  }
  return entries
})

const actionableSessionCount = computed(() => actionableSessions.value.length)

const onListEntrySelect = (entry: ActionableSessionEntry) => {
  showListDialog.value = false
  emit("select", { mode: entry.mode, session: entry.session })
}
</script>

<style lang="scss" scoped>
.learning-session-actions-btn {
  position: relative;
  padding: 0.75rem 1rem;
  min-height: 2.5rem;
  svg {
    width: 32px;
    height: 32px;
  }
}

.learning-session-actions-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  border-radius: 50%;
  min-width: 16px;
  height: 16px;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  color: white;
  background: #66b0ff;
}
</style>
