<template>
  <Modal :is-popup="true" @close_request="$emit('close')">
    <template #header>
      <h2>Learning session actions</h2>
    </template>
    <template #body>
      <div
        class="flex flex-col gap-2"
        data-test="learning-session-list-dialog"
      >
        <p
          v-if="entries.length === 0"
          class="text-sm text-base-content"
          data-test="learning-session-list-empty"
        >
          No learning sessions yet.
        </p>
        <button
          v-for="entry in entries"
          :key="entry.key"
          type="button"
          class="daisy-btn daisy-btn-outline learning-session-action-entry"
          data-test="learning-session-action-entry"
          @click="$emit('select', entry)"
        >
          {{ entry.notebookName }} — {{ entry.actionLabel }}
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import Modal from "@/components/commons/Modal.vue"
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

defineProps<{
  entries: ActionableSessionEntry[]
}>()

defineEmits<{
  (e: "close"): void
  (e: "select", entry: ActionableSessionEntry): void
}>()
</script>

<style lang="scss" scoped>
.learning-session-action-entry {
  justify-content: flex-start;
}
</style>
