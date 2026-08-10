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
          v-if="sessions.length === 0"
          class="text-sm text-base-content"
          data-test="learning-session-list-empty"
        >
          No learning sessions yet.
        </p>
        <button
          v-for="session in sessions"
          :key="session.notebookId"
          type="button"
          class="daisy-btn daisy-btn-outline learning-session-action-entry"
          data-test="learning-session-action-entry"
          @click="$emit('select', session)"
        >
          {{ session.notebookName }} — Request
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import Modal from "@/components/commons/Modal.vue"
import type { PotentialLearningSession } from "@/composables/useRecallData"

defineProps<{
  sessions: PotentialLearningSession[]
}>()

defineEmits<{
  (e: "close"): void
  (e: "select", session: PotentialLearningSession): void
}>()
</script>

<style lang="scss" scoped>
.learning-session-action-entry {
  justify-content: flex-start;
}
</style>
