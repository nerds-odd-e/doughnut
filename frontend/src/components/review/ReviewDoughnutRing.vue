<template>
  <svg class="doughnut-ring" viewBox="-50 -50 100 100" width="100" height="100">
    <ReviewDoughnutRingPiece
      class="initial-review"
      name="initial"
      :start-point="startPoint"
      :big-arc="true"
      :end-point="endPoint"
      :text="`${reviewing.toInitialReviewCount}/${reviewing.notLearntCount}`"
    />
    <ReviewDoughnutRingPiece
      class="repeat-review"
      name="repeat"
      :big-arc="false"
      :start-point="endPoint"
      :end-point="startPoint"
      :text="`${reviewing.toRepeatCount}/${reviewing.learntCount}`"
    />
  </svg>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ReviewDoughnutRingPiece from "./ReviewDoughnutRingPiece.vue";

const radius = 40;

export default defineComponent({
  props: {
    reviewing: {
      type: Object as PropType<Generated.ReviewStatus>,
      required: true,
    },
  },
  computed: {
    startPoint() {
      return `0 ${-radius}`;
    },
    endPoint() {
      const angle =
        (Math.PI * this.reviewing.toInitialReviewCount) /
        (this.reviewing.toInitialReviewCount + this.reviewing.toRepeatCount);
      return `${(Math.cos(angle) * radius).toFixed(2)} ${(
        Math.sin(angle) * radius
      ).toFixed(2)}`;
    },
  },
  components: { ReviewDoughnutRingPiece },
});
</script>

<style lang="scss" scoped>
.doughnut-ring {
  font-size: 0.8rem;
  font-weight: bold;
  color: #fff;
  background-color: #000;
  padding: 0.2rem 0.5rem;
  border-radius: 0.5rem;
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  // transform: rotate(45deg);
}

.initial-review {
  stroke: #3bafda;
}

.repeat-review {
  stroke: #3baf3a;
}
</style>
