<template>
  <ContainerPage v-bind="{ loading, contentExists: !!reviewPointViewedByUser }">
    <template v-if="!!reviewPoint">
      <ProgressBar
        :class="nested ? 'initial-review-paused' : ''"
        v-bind="{
          title: `Initial Review: `,
          finished,
          toRepeatCount: reviewPointViewedByUser.remainingInitialReviewCountForToday,
        }"
      >
        <template #default v-if="nested">
          <div style="display: flex" @click="resume">
            <a title="Go back to review">
              <SvgResume width="30" height="30" />
            </a>
            <ReviewPointAbbr v-bind="{ reviewPointViewedByUser }" />
          </div>
        </template>
      </ProgressBar>
      <template v-if="!nested">
        <ShowReviewPoint v-bind="{ reviewPointViewedByUser }" />
        <div>
          <div class="mb-2">
            <ReviewSettingForm
              v-if="!!reviewPointViewedByUser.reviewSetting"
              v-model="reviewSetting"
              :showLevel="false"
              :errors="{}"
            />
          </div>
          <InitialReviewButtons :key="buttonKey" @doInitialReview="processForm($event)" />
        </div>
      </template>
    </template>
  </ContainerPage>
</template>

<script>
import ShowReviewPoint from "../components/review/ShowReviewPoint.vue";
import ReviewSettingForm from "../components/review/ReviewSettingForm.vue";
import ReviewPointAbbr from "../components/review/ReviewPointAbbr.vue";
import InitialReviewButtons from "../components/review/InitialReviewButtons.vue";
import ProgressBar from "../components/commons/ProgressBar.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import usePopups from "../components/commons/Popups/usePopup";
import SvgResume from "../components/svgs/SvgResume.vue";

export default {
  setup() {
    return { ...useStoredLoadingApi(), ...usePopups() };
  },
  name: "InitialReviewPage",
  props: { nested: Boolean },
  components: {
    ShowReviewPoint,
    ReviewSettingForm,
    ContainerPage,
    InitialReviewButtons,
    ProgressBar,
    ReviewPointAbbr,
    SvgResume,
  },
  data() {
    return {
      finished: 0,
      reviewPointViewedByUser: null,
    };
  },
  computed: {
    reviewPoint() {
      return this.reviewPointViewedByUser.reviewPoint;
    },
    reviewSetting() {
      return this.reviewPointViewedByUser.reviewSetting;
    },
    buttonKey() {
      return !!this.reviewPoint.noteId
        ? `note-${this.reviewPoint.noteId}`
        : `link-${this.reviewPoint.linkId}`;
    },
  },

  methods: {
    resume() {
      this.$router.push({ name: "initial" });
    },
    loadNew(resp) {
      this.reviewPointViewedByUser = resp;
      if (!this.reviewPointViewedByUser.reviewPoint) {
        this.$router.push({ name: "reviews" });
      }
    },

    async processForm(skipReview) {
      this.finished += 1;
      this.reviewPointViewedByUser.remainingInitialReviewCountForToday -= 1;
      if (skipReview) {
        if (
          !(await this.popups.confirm(
            "Confirm to hide this note from reviewing in the future?"
          ))
        )
          return;
      }
      this.reviewPoint.removedFromReview = skipReview;
      this.storedApi.reviewMethods
        .doInitialReview({
          reviewPoint: this.reviewPoint,
          reviewSetting: this.reviewSetting,
        })
        .then(this.loadNew);
    },

    fetchData() {
      this.storedApi.reviewMethods.getOneInitialReview().then(this.loadNew);
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>

<style>
.initial-review-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>
