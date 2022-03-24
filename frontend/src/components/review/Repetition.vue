<template>
  <template v-if="compact">
    <ReviewPointAbbr v-bind="{ reviewPointViewedByUser }" />
    <SelfEvaluateButtons
      v-bind="{ sadButton }"
      :key="buttonKey"
      @selfEvaluate="selfEvaluate($event)"
    />
  </template>
  <template v-else>
    <AnswerResult v-if="answerResult" v-bind="{answerResult}"/>
    <ShowReviewPoint
      v-bind="{ reviewPointViewedByUser }"
    />

    <div class="btn-toolbar justify-content-between">
      <SelfEvaluateButtons
        v-bind="{ sadButton }"
        :key="buttonKey"
        @selfEvaluate="selfEvaluate($event)"
      />
      <button
        class="btn"
        title="remove this note from review"
        @click="$emit('removeFromReview')"
      >
        <SvgNoReview />
      </button>
    </div>
  </template>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import SvgCog from "../svgs/SvgCog.vue";
import SvgNoReview from "../svgs/SvgNoReview.vue";
import ShowReviewPoint from "./ShowReviewPoint.vue";
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue";
import ReviewPointAbbr from "./ReviewPointAbbr.vue";
import AnswerResult from "./AnswerResult.vue";

export default defineComponent({
  name: "Repetition",
  props: {
    reviewPointViewedByUser: { type: Object as PropType<Generated.ReviewPointViewedByUser>, required: true },
    answerResult: Object as PropType<Generated.AnswerResult>,
    compact: Boolean,
  },
  emits: ["selfEvaluate", "removeFromReview"],
  components: {
    SvgCog,
    SvgNoReview,
    ShowReviewPoint,
    SelfEvaluateButtons,
    ReviewPointAbbr,
    AnswerResult,
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
