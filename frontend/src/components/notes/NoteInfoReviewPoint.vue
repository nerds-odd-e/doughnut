<template>
  <div class="alert alert-danger" v-if="localReviewPoint.removedFromReview">
    This review point has been removed from reviewing.
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
import { ReviewPoint } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { PropType, ref, watch } from "vue"
import usePopups from "../commons/Popups/usePopups"

const props = defineProps({
  modelValue: {
    type: Object as PropType<ReviewPoint>,
    required: true,
  },
})

const emit = defineEmits(["update:modelValue"])

const localReviewPoint = ref<ReviewPoint>(props.modelValue)
const { managedApi } = useLoadingApi()
const { popups } = usePopups()

watch(
  () => props.modelValue,
  (newVal) => {
    localReviewPoint.value = newVal
  },
  { immediate: true },
)

const selfEvaluate = async (adjustment: number) => {
  const reviewPoint = await managedApi.restReviewPointController.selfEvaluate(
    localReviewPoint.value.id,
    {
      adjustment,
    },
  )
  localReviewPoint.value = reviewPoint
  emit("update:modelValue", reviewPoint)
}

const removeFromReview = async () => {
  if (
    !(await popups.confirm(
      `Confirm to hide this from reviewing in the future?`,
    ))
  ) {
    return
  }
  const reviewPoint =
    await managedApi.restReviewPointController.removeFromRepeating(
      localReviewPoint.value.id,
    )
  localReviewPoint.value = reviewPoint
  emit("update:modelValue", reviewPoint)
}
</script>
