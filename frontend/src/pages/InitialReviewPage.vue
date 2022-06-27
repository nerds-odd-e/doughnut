<template>
  <ContainerPage
    v-bind="{
      loading,
      contentExists: reviewPoints !== undefined,
    }"
  >
    <ProgressBar
      :class="nested ? 'initial-review-paused' : ''"
      v-bind="{
        title: `Initial Review: `,
        finished,
        toRepeatCount: remainingInitialReviewCountForToday,
      }"
    >
      <template #default v-if="nested">
        <div style="display: flex" @click="resume">
          <a title="Go back to review">
            <SvgResume width="30" height="30" />
          </a>
          <ReviewPointAbbr v-bind="{ reviewPoint }" />
        </div>
      </template>
    </ProgressBar>
    <InitialReview
      v-if="reviewPoint"
      v-bind="{ nested, reviewPoint }"
      @initial-review-done="initialReviewDone"
      @level-changed="levelChanged"
      :key="reviewPoint?.thing?.id"
    />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ContainerPage from "./commons/ContainerPage.vue";
import ProgressBar from "../components/commons/ProgressBar.vue";
import SvgResume from "../components/svgs/SvgResume.vue";
import ReviewPointAbbr from "../components/review/ReviewPointAbbr.vue";
import InitialReview from "../components/review/InitialReview.vue";
import useLoadingApi from "../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: { nested: Boolean },
  components: {
    ContainerPage,
    InitialReview,
    ProgressBar,
    ReviewPointAbbr,
    SvgResume,
  },
  data() {
    return {
      finished: 0,
      reviewPoints: [] as Generated.ReviewPoint[],
    };
  },
  computed: {
    reviewPoint() {
      return this.reviewPoints[this.finished];
    },
    remainingInitialReviewCountForToday() {
      return this.reviewPoints.length - this.finished;
    },
  },
  methods: {
    resume() {
      this.$router.push({ name: "initial" });
    },
    initialReviewDone() {
      if (this.finished + 1 === this.reviewPoints.length) {
        this.$router.push({ name: "reviews" });
        return;
      }
      this.finished += 1;
    },
    levelChanged() {
      this.loadInitialReview();
    },
    loadInitialReview() {
      this.api.reviewMethods.initialReview().then((resp) => {
        if (resp.length === 0) {
          this.$router.push({ name: "reviews" });
          return;
        }
        this.reviewPoints = resp;
      });
    },
  },
  mounted() {
    this.loadInitialReview();
  },
});
</script>
