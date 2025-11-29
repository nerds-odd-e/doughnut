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
  <td @click.stop>
    <div class="btn-group" role="group" aria-label="First group">
      <button
        class="btn"
        name="sad"
        @click="selfEvaluateHandler(-5)"
        title="reduce next repeat interval (days) by half"
      >
        <SvgSad />
      </button>
      <button
        class="btn"
        name="happy"
        @click="selfEvaluateHandler(5)"
        title="add to next repeat interval (days) by half"
      >
        <SvgHappy />
      </button>
      <button
        class="btn"
        title="remove this note from review"
        @click="removeFromReview"
      >
        <SvgNoReview />
      </button>
    </div>
  </td>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch } from "vue"
import type { MemoryTracker } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import usePopups from "../commons/Popups/usePopups"
import SvgNoReview from "../svgs/SvgNoReview.vue"
import SvgSad from "../svgs/SvgSad.vue"
import SvgHappy from "../svgs/SvgHappy.vue"

const props = defineProps({
  modelValue: {
    type: Object as PropType<MemoryTracker>,
    required: true,
  },
})

const emit = defineEmits(["update:modelValue"])

const localMemoryTracker = ref<MemoryTracker>(props.modelValue)
const { popups } = usePopups()

watch(
  () => props.modelValue,
  (newVal) => {
    localMemoryTracker.value = newVal
  },
  { immediate: true }
)

const selfEvaluateHandler = async (adjustment: number) => {
  const { data: memoryTracker, error } = await apiCallWithLoading(() =>
    MemoryTrackerController.selfEvaluate({
      path: { memoryTracker: localMemoryTracker.value.id },
      body: {
        adjustment,
      },
    })
  )
  if (!error) {
    localMemoryTracker.value = memoryTracker!
    emit("update:modelValue", memoryTracker!)
  }
}

const removeFromReview = async () => {
  if (
    !(await popups.confirm(
      `Confirm to hide this from reviewing in the future?`
    ))
  ) {
    return
  }
  const { data: memoryTracker, error } = await apiCallWithLoading(() =>
    MemoryTrackerController.removeFromRepeating({
      path: { memoryTracker: localMemoryTracker.value.id },
    })
  )
  if (!error) {
    localMemoryTracker.value = memoryTracker!
    emit("update:modelValue", memoryTracker!)
  }
}
</script>
