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
      v-if="potentialLearningSessions.length > 0"
      class="learning-session-actions-badge"
      data-test="learning-session-actions-badge"
    >
      {{ potentialLearningSessions.length }}
    </div>
  </button>
  <LearningSessionListDialog
    v-if="showListDialog"
    :sessions="potentialLearningSessions"
    @close="showListDialog = false"
    @select="onSelect"
  />
</template>

<script setup lang="ts">
import { ref } from "vue"
import { GraduationCap } from "@lucide/vue"
import type { PotentialLearningSession } from "@/composables/useRecallData"
import LearningSessionListDialog from "./LearningSessionListDialog.vue"

defineProps<{
  potentialLearningSessions: PotentialLearningSession[]
}>()

const emit = defineEmits<{
  (e: "select", session: PotentialLearningSession): void
}>()

const showListDialog = ref(false)

const onSelect = (session: PotentialLearningSession) => {
  showListDialog.value = false
  emit("select", session)
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
