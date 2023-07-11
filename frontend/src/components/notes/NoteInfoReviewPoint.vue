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

<script lang="ts">
import { defineComponent, PropType } from "vue";
import SvgNoReview from "../svgs/SvgNoReview.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopups";
import SvgSad from "../svgs/SvgSad.vue";
import SvgHappy from "../svgs/SvgHappy.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  props: {
    reviewPoint: {
      type: Object as PropType<Generated.ReviewPoint>,
      required: true,
    },
  },
  emits: ["selfEvaluated"],
  components: { SvgNoReview, SvgSad, SvgHappy },
  methods: {
    selfEvaluate(adjustment: number) {
      this.api.reviewMethods
        .selfEvaluate(this.reviewPoint.id, {
          adjustment,
        })
        .then((reviewPoint) => {
          this.$emit("selfEvaluated", reviewPoint);
        });
    },

    async removeFromReview() {
      if (
        !(await this.popups.confirm(
          `Confirm to hide this from reviewing in the future?`,
        ))
      ) {
        return;
      }
      this.api.reviewMethods
        .removeFromReview(this.reviewPoint.id)
        .then((reviewPoint) => {
          this.$emit("selfEvaluated", reviewPoint);
        });
    },
  },
});
</script>
