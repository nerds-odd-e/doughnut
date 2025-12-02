<template>
  <td :class="{ 'strikethrough': isSkipped }">
    {{ localMemoryTracker.spelling ? 'spelling' : 'normal' }}
  </td>
  <td :class="{ 'strikethrough': isSkipped }">
    <span class="statistics-value">{{ localMemoryTracker.repetitionCount }}</span>
  </td>
  <td :class="{ 'strikethrough': isSkipped }">
    <span class="statistics-value">{{ localMemoryTracker.forgettingCurveIndex }}</span>
  </td>
  <td :class="{ 'strikethrough': isSkipped }">
    <span class="statistics-value">{{ new Date(localMemoryTracker.nextRecallAt).toLocaleString() }}</span>
  </td>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch, computed } from "vue"
import type { MemoryTracker } from "@generated/backend"

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
