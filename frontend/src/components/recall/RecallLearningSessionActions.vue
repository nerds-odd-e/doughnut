<template>
  <div class="learning-session-actions">
    <button
      ref="actionsButtonRef"
      type="button"
      class="daisy-btn large-btn learning-session-actions-btn"
      title="Learning session actions"
      data-test="learning-session-actions"
      @click="onSessionActionsClick"
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
    <Teleport to="body">
      <div
        v-if="showActionPicker"
        class="learning-session-actions-picker"
        data-test="learning-session-actions-picker"
        :style="pickerStyle"
      >
        <p class="learning-session-actions-picker-title">
          Learning session actions
        </p>
        <button
          v-for="entry in actionableSessions"
          :key="entry.key"
          type="button"
          class="daisy-btn daisy-btn-outline learning-session-action-entry"
          data-test="learning-session-action-entry"
          @click="onPickerEntryClick(entry)"
        >
          {{ entry.notebookName }} — {{ entry.actionLabel }}
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, type CSSProperties } from "vue"
import { GraduationCap } from "@lucide/vue"
import type { LearningSessionLite } from "@generated/doughnut-backend-api"
import type { PotentialLearningSession } from "@/composables/useRecallData"

export type LearningSessionActionMode = "commission" | "record" | "amend"

export type ActionableSessionEntry = {
  key: string
  mode: LearningSessionActionMode
  notebookName: string
  session: PotentialLearningSession | LearningSessionLite
  actionLabel: string
}

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

const showActionPicker = ref(false)
const actionsButtonRef = ref<HTMLButtonElement | null>(null)
const pickerStyle = ref<CSSProperties>({})

const actionableSessions = computed((): ActionableSessionEntry[] => {
  const entries: ActionableSessionEntry[] = []
  for (const session of props.potentialLearningSessions) {
    entries.push({
      key: `potential-${session.notebookId}`,
      mode: "commission",
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

const onSessionActionsClick = () => {
  if (!showActionPicker.value && actionsButtonRef.value) {
    const rect = actionsButtonRef.value.getBoundingClientRect()
    pickerStyle.value = {
      top: `${rect.bottom + 8}px`,
      left: `${Math.max(8, rect.right - 256)}px`,
    }
  }
  showActionPicker.value = !showActionPicker.value
}

const onPickerEntryClick = (entry: ActionableSessionEntry) => {
  emit("select", { mode: entry.mode, session: entry.session })
  showActionPicker.value = false
}
</script>

<style lang="scss" scoped>
.learning-session-actions {
  position: relative;
}

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

.learning-session-actions-picker {
  position: fixed;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 16rem;
  padding: 0.75rem;
  border-radius: 0.5rem;
  background: var(--color-base-200);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.learning-session-actions-picker-title {
  font-size: 0.875rem;
  font-weight: 600;
}

.learning-session-action-entry {
  justify-content: flex-start;
}
</style>
