<template>
  <template v-if="compact">
    <ReviewPointAbbr v-bind="{ noteWithPosition: note, linkViewedByUser }" />
    <SelfEvaluateButtons
      v-bind="{ sadButton }"
      :key="buttonKey"
      @selfEvaluate="selfEvaluate($event)"
    />
  </template>
  <template v-else>
    <div v-if="answerResult">
      <div class="alert alert-success" v-if="answerResult.correct">
        Correct!
      </div>
      <div class="alert alert-danger" v-else>
        {{ "Your answer `" + answerResult.answerDisplay + "` is wrong." }}
      </div>
    </div>

    <ShowReviewPoint
      v-bind="{ noteWithPosition: note, linkViewedByUser }"
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
        @click="removeFromReview()"
      >
        <SvgNoReview />
      </button>
    </div>
  </template>
</template>

<script>
import SvgCog from "../svgs/SvgCog.vue";
import SvgNoReview from "../svgs/SvgNoReview.vue";
import ShowReviewPoint from "./ShowReviewPoint.vue";
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue";
import ReviewPointAbbr from "./ReviewPointAbbr.vue";
import { restPost } from "../../restful/restful";

export default {
  name: "Repetition",
  props: {
    reviewPoint: Object,
    answerResult: Object,
    noteWithPosition: Object,
    linkViewedByUser: Object,
    compact: Boolean,
  },
  emits: ["selfEvaluate", "reviewPointRemoved"],
  components: {
    SvgCog,
    SvgNoReview,
    ShowReviewPoint,
    SelfEvaluateButtons,
    ReviewPointAbbr,
  },
  data() {
    return {
      loading: false,
      buttonKey: 1,
    };
  },
  computed: {
    sadButton() {
      return !!this.answerResult && !this.answerResult.correct;
    },
    note() {
      return this.noteWithPosition
    }
  },
  methods: {
    selfEvaluate(event) {
      this.buttonKey += 1;
      this.$emit("selfEvaluate", event);
    },

    async removeFromReview() {
      if (
        !(await this.$popups.confirm(
          `Are you sure to hide this from reviewing in the future?`
        ))
      ) {
        return;
      }
      restPost(
        `/api/review-points/${this.reviewPoint.id}/remove`,
        {},
        (r) => (this.loading = r)
      ).then((r) => this.$emit("reviewPointRemoved"));
    },
  },
};
</script>
