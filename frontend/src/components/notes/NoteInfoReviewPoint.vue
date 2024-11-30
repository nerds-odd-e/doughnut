<template>
  <div class="alert alert-danger" v-if="localReviewPoint.removedFromReview">
    This memory tracker has been removed from reviewing.
  </div>
  <label
    >Repetition Count:
    <span class="statistics-value">{{
      localReviewPoint.repetitionCount
    }}</span></label
  >
  <label
    >Forgetting Curive Index:
    <span class="statistics-value">{{
      localReviewPoint.forgettingCurveIndex
    }}</span></label
  >
  <label
    >Next Review:
    <span class="statistics-value">{{
      new Date(localReviewPoint.nextReviewAt).toLocaleString()
    }}</span></label
  >
  <div class="btn-group" role="group" aria-label="First group">
    <button
      class="btn"
      name="sad"
      @click="selfEvaluate(-5)"
      title="reduce next repeat interval (days) by half"
    >
      <SvgSad />
    </button>
    <button
      class="btn"
      name="happy"
      @click="selfEvaluate(5)"
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
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, watch } from "vue"
import type { MemoryTracker } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
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

const localReviewPoint = ref<MemoryTracker>(props.modelValue)
const { managedApi } = useLoadingApi()
const { popups } = usePopups()

watch(
  () => props.modelValue,
  (newVal) => {
    localReviewPoint.value = newVal
  },
  { immediate: true }
)

const selfEvaluate = async (adjustment: number) => {
  const memoryTracker = await managedApi.restReviewPointController.selfEvaluate(
    localReviewPoint.value.id,
    {
      adjustment,
    }
  )
  localReviewPoint.value = memoryTracker
  emit("update:modelValue", memoryTracker)
}

const removeFromReview = async () => {
  if (
    !(await popups.confirm(
      `Confirm to hide this from reviewing in the future?`
    ))
  ) {
    return
  }
  const memoryTracker =
    await managedApi.restReviewPointController.removeFromRepeating(
      localReviewPoint.value.id
    )
  localReviewPoint.value = memoryTracker
  emit("update:modelValue", memoryTracker)
}
</script>
