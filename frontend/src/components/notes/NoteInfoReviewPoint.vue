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
  <div class="btn-toolbar justify-content-between">
    <SelfEvaluateButtons @self-evaluate="selfEvaluate" />
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
import SelfEvaluateButtons from "../review/SelfEvaluateButtons.vue";
import SvgNoReview from "../svgs/SvgNoReview.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopup";

export default defineComponent({
  setup() {
    return { ...useLoadingApi({ initalLoading: true }), ...usePopups() };
  },
  props: {
    reviewPoint: {
      type: Object as PropType<Generated.ReviewPoint>,
      required: true,
    },
  },
  emits: ["selfEvaluated"],
  components: { SelfEvaluateButtons, SvgNoReview },
  methods: {
    selfEvaluate(data: Generated.SelfEvaluate) {
      this.api.reviewMethods
        .selfEvaluate(this.reviewPoint.id, {
          selfEvaluation: data,
        })
        .then((reviewPoint) => {
          this.$emit("selfEvaluated", reviewPoint);
        });
    },

    async removeFromReview() {
      if (
        !(await this.popups.confirm(
          `Confirm to hide this from reviewing in the future?`
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
