<template>
  <LoadingPage v-bind="{ loading, contentExists: !!reviewPointViewedByUser }">
    <ShowReviewPoint
      v-if="reviewPointViewedByUser"
      v-bind="{
        reviewPointViewedByUser,
      }"
      :key="reviewPointId"
    />
    <div class="btn-toolbar justify-content-between">
      <label v-if="nextReviewAt" v-text="nextReviewAt" />
      <template v-else>
        <SelfEvaluateButtons @selfEvaluate="selfEvaluate" />
        <button
          class="btn"
          title="remove this note from review"
          @click="removeFromReview"
        >
          <SvgNoReview />
        </button>
      </template>
    </div>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import ShowReviewPoint from "./ShowReviewPoint.vue";
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue";
import SvgNoReview from "../svgs/SvgNoReview.vue";
import usePopups from "../commons/Popups/usePopup";

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi({ initalLoading: true }), ...usePopups() };
  },
  props: {
    reviewPointId: { type: Number, required: true },
  },
  components: {
    LoadingPage,
    ShowReviewPoint,
    SelfEvaluateButtons,
    SvgNoReview,
  },
  emits: ["selfEvaluated"],
  data() {
    return {
      reviewPointViewedByUser: undefined as
        | Generated.ReviewPointViewedByUser
        | undefined,
      nextReviewAt: undefined as string | undefined,
    };
  },
  methods: {
    selfEvaluate(data: string) {
      this.storedApi.reviewMethods
        .selfEvaluate(this.reviewPointId, {
          selfEvaluation: data,
        })
        .then((reviewPoint) => {
          this.nextReviewAt = reviewPoint.nextReviewAt;
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
        .removeFromReview(this.reviewPointId)
        .then((r) => this.fetchData());
    },

    async fetchData() {
      this.reviewPointViewedByUser =
        await this.storedApi.reviewMethods.getReviewPoint(this.reviewPointId);
    },
  },
  watch: {
    reviewPointId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
