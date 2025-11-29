<template>
  <td>{{ localMemoryTracker.spelling ? 'spelling' : 'normal' }}</td>
  <td>
    <span class="statistics-value">{{ localMemoryTracker.repetitionCount }}</span>
    <div class="alert alert-danger" v-if="localMemoryTracker.removedFromTracking">
      This memory tracker has been removed from tracking.
    </div>
  </td>
  <td>
    <span class="statistics-value">{{ localMemoryTracker.forgettingCurveIndex }}</span>
  </td>
  <td>
    <span class="statistics-value">{{ new Date(localMemoryTracker.nextRecallAt).toLocaleString() }}</span>
  </td>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch } from "vue"
import type { MemoryTracker } from "@generated/backend"

const props = defineProps({
  modelValue: {
    type: Object as PropType<MemoryTracker>,
    required: true,
  },
})

const emit = defineEmits(["update:modelValue"])

const localMemoryTracker = ref<MemoryTracker>(props.modelValue)

watch(
  () => props.modelValue,
  (newVal) => {
    localMemoryTracker.value = newVal
  },
  { immediate: true }
)
</script>
