<template>
    <ShowReviewPoint
      v-bind="{ reviewPointViewedByUser }"
    />

</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import SvgCog from "../svgs/SvgCog.vue";
import SvgNoReview from "../svgs/SvgNoReview.vue";
import ShowReviewPoint from "./ShowReviewPoint.vue";
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue";
import ReviewPointAbbr from "./ReviewPointAbbr.vue";

export default defineComponent({
  name: "Repetition",
  props: {
    reviewPointViewedByUser: { type: Object as PropType<Generated.ReviewPointViewedByUser>, required: true },
    answerResult: Object as PropType<Generated.AnswerResult>,
  },
  emits: ["selfEvaluate", "removeFromReview"],
  components: {
    SvgCog,
    SvgNoReview,
    ShowReviewPoint,
    SelfEvaluateButtons,
    ReviewPointAbbr,
  },
  data() {
    return {
      buttonKey: 1,
    };
  },
  computed: {
    sadButton() {
      return this.answerResult && !this.answerResult.correct;
    },
  },
  methods: {
    selfEvaluate(event: string) {
      this.buttonKey += 1;
      this.$emit("selfEvaluate", event);
    },

  },
});
</script>
