<template>
  <svg class="doughnut-ring" viewBox="0 0 100 100" width="100" height="100">
    <g class="initial-review">
      <path
        id="initial-curve"
        d="M50 10,A40 40 3 1 1,10 50"
        stroke="#3BAFDA"
        fill="none"
        stroke-width="20"
      />

      <text>
        <textPath
          startOffset="50%"
          text-anchor="middle"
          alignment-baseline="middle"
          xlink:href="#initial-curve"
        >
          {{ `${reviewing.toInitialReviewCount}/${reviewing.notLearntCount}` }}
        </textPath>
      </text>
    </g>
  </svg>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  props: {
    reviewing: {
      type: Object as PropType<Generated.ReviewStatus>,
      required: true,
    },
  },
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
}
</style>
