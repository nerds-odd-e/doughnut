<template>
  <svg class="doughnut-ring" viewBox="-50 -50 100 100" width="100" height="100">
    <g class="doughnut-ring-pieces">
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
    </g>
    <UserIconMenu class="user-icon-menu" />
  </svg>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ReviewDoughnutRingPiece from "./ReviewDoughnutRingPiece.vue";
import UserIconMenu from "../toolbars/UserIconMenu.vue";

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
    flooredToInitialReviewCount() {
      return Math.max(1, this.reviewing.toInitialReviewCount);
    },
    flooredToRepeatCount() {
      return Math.max(1, this.reviewing.toRepeatCount);
    },
  },
  components: { ReviewDoughnutRingPiece, UserIconMenu },
});
</script>

<style lang="scss" scoped>
.doughnut-ring {
  font-size: 0.8rem;
  font-weight: bold;
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
}
.doughnut-ring-pieces {
  transform: rotate(45deg);
}

.initial-review {
  stroke: #3bafda;
}

.repeat-review {
  stroke: #3baf3a;
}
</style>
