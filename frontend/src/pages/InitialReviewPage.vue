<template>
  <ContainerPage v-bind="{ contentExists: !!reviewPoint }">
    <ProgressBar
      :class="minimized ? 'initial-review-paused' : ''"
      v-bind="{
        title: `Initial Review: `,
        finished,
        toRepeatCount: remainingInitialReviewCountForToday,
      }"
    >
      <template #default v-if="minimized">
        <div style="display: flex" @click="resume">
          <a title="Go back to review">
            <SvgResume width="30" height="30" />
          </a>
          <ReviewPointAbbr v-bind="{ reviewPoint }" />
        </div>
      </template>
    </ProgressBar>
    <InitialReview
      v-if="!minimized && reviewPoint"
      v-bind="{ reviewPoint, storageAccessor }"
      @initial-review-done="initialReviewDone"
      @reload-needed="onReloadNeeded"
      :key="reviewPoint.thing.id"
    />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ContainerPage from "./commons/ContainerPage.vue";
import ProgressBar from "../components/commons/ProgressBar.vue";
import SvgResume from "../components/svgs/SvgResume.vue";
import ReviewPointAbbr from "../components/review/ReviewPointAbbr.vue";
import InitialReview from "../components/review/InitialReview.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  name: "InitialReviewPage",
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    minimized: Boolean,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    ContainerPage,
    InitialReview,
    ProgressBar,
    ReviewPointAbbr,
    SvgResume,
  },
  emits: ["update-reviewing"],
  data() {
    return {
      finished: 0,
      reviewPoints: [] as Generated.ReviewPoint[],
    };
  },
  computed: {
    reviewPoint() {
      return this.reviewPoints[0];
    },
    remainingInitialReviewCountForToday() {
      return this.reviewPoints.length;
    },
  },
  methods: {
    resume() {
      this.$router.push({ name: "initial" });
    },
    initialReviewDone() {
      this.finished += 1;
      this.reviewPoints.shift();
      if (this.reviewPoints.length === 0) {
        this.$router.push({ name: "reviews" });
        return;
      }
    },
    onReloadNeeded() {
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
