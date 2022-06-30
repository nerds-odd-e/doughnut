<template>
  <label
    >Repetition Count:
    <span class="statistics-value">{{
      reviewPoint.repetitionCount
    }}</span></label
  >
  <label
    >Forgetting Curive Index:
    <span class="statistics-value">{{
      reviewPoint.forgettingCurveIndex
    }}</span></label
  >
  <label
    >Next Review:
    <span class="statistics-value">{{
      new Date(reviewPoint.nextReviewAt).toLocaleString()
    }}</span></label
  >
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  props: {
    reviewPoint: {
      type: Object as PropType<Generated.ReviewPoint>,
      required: true,
    },
  },
});
</script>
