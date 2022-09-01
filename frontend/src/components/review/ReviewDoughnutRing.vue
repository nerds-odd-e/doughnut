<template>
  <svg class="doughnut-ring" viewBox="-50 -50 100 100" width="100" height="100">
    <g class="doughnut-ring-pieces">
      <ReviewDoughnutRingPiece
        role="button"
        class="ring-piece initial-review"
        name="initial"
        :start-point="startPoint"
        :big-arc="moreTInitialReview"
        :end-point="endPoint"
        title="Initial Review"
        :text="`${reviewing.toInitialReviewCount}/${reviewing.notLearntCount}`"
        @click="$router.push({ name: 'initial' })"
      />
      <ReviewDoughnutRingPiece
        role="button"
        class="ring-piece repeat-review"
        name="repeat"
        :big-arc="!moreTInitialReview"
        :start-point="endPoint"
        :end-point="startPoint"
        title="Repeat Old"
        :text="`${reviewing.toRepeatCount}/${reviewing.learntCount}`"
        @click="$router.push({ name: 'repeat' })"
      />
    </g>
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
        (Math.PI * this.flooredToInitialReviewCount) /
        (this.flooredToInitialReviewCount + this.flooredToRepeatCount);
      return `${(Math.cos(angle) * radius).toFixed(2)} ${(
        Math.sin(angle) * radius
      ).toFixed(2)}`;
    },
    moreTInitialReview() {
      return this.flooredToInitialReviewCount > this.flooredToRepeatCount;
    },
    flooredToInitialReviewCount() {
      return Math.max(1, this.reviewing.toInitialReviewCount);
    },
    flooredToRepeatCount() {
      return Math.max(1, this.reviewing.toRepeatCount);
    },
  },
  components: { ReviewDoughnutRingPiece },
});
</script>

<style lang="scss" scoped>
.doughnut-ring {
  pointer-events: none;
  font-size: 0.8rem;
  font-weight: bold;
  position: absolute;
  top: 6.5rem;
  right: 0.5rem;
}
.doughnut-ring-pieces {
  pointer-events: visiblePainted;
  transform: rotate(45deg);
}

.initial-review {
  stroke: #3bafda;
}

.ring-piece {
  opacity: 0.5;
  &:hover {
    opacity: 1;
  }
}

.repeat-review {
  stroke: #3baf3a;
}
</style>
