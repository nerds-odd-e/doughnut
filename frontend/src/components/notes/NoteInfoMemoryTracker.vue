<template>
  <tr>
    <td :class="{ 'strikethrough': isSkipped }">
      <div>{{ trackerTypeLabel }}</div>
      <div
        v-if="tutorFeedbackGrade !== undefined"
        :data-test="`tutor-feedback-grade-${tutorFeedbackGrade}`"
        class="text-sm mt-1"
      >
        tutor feedback grade {{ tutorFeedbackGrade }} from a learning session
      </div>
    </td>
    <td :class="{ 'strikethrough': isSkipped }">
      <span class="statistics-value">{{ localMemoryTracker.recallCount }}</span>
    </td>
    <td :class="{ 'strikethrough': isSkipped }">
      <span class="statistics-value">{{ localMemoryTracker.stability }}</span>
    </td>
    <td :class="{ 'strikethrough': isSkipped }">
      <span class="statistics-value">{{ new Date(localMemoryTracker.nextRecallAt).toLocaleString() }}</span>
    </td>
  </tr>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch, computed } from "vue"
import type { MemoryTracker } from "@generated/doughnut-backend-api"

const props = defineProps({
  modelValue: {
    type: Object as PropType<MemoryTracker>,
    required: true,
  },
})

const emit = defineEmits(["update:modelValue"])

const localMemoryTracker = ref<MemoryTracker>(props.modelValue)

const isSkipped = computed(
  () => localMemoryTracker.value.removedFromTracking === true
)

const trackerTypeLabel = computed(() => {
  const { propertyKey, spelling, type } = localMemoryTracker.value
  if (type === "COMMISSIONED") {
    return "Commissioned"
  }
  if (propertyKey) {
    return `property: ${propertyKey}`
  }
  return spelling ? "spelling" : "normal"
})

const tutorFeedbackGrade = computed(() => {
  const { type, latestTutorFeedbackGrade } = localMemoryTracker.value
  if (type !== "COMMISSIONED" || latestTutorFeedbackGrade == null) {
    return undefined
  }
  return latestTutorFeedbackGrade
})

watch(
  () => props.modelValue,
  (newVal) => {
    localMemoryTracker.value = newVal
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.strikethrough {
  text-decoration: line-through;
  opacity: 0.6;
}
</style>
