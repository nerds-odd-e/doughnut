<template>
  <ContainerPage v-bind="{ loading, contentExists: !!reviewPointViewedByUser }">
    <template v-if="!!reviewPoint">
      <ProgressBar
        v-bind="{
          title: `Initial Review: `,
          finished,
          toRepeatCount:
            reviewPointViewedByUser.remainingInitialReviewCountForToday,
        }"
      >
      </ProgressBar>
      <Minimizable :minimized="nested">
        <template #minimizedContent>
          <div
            class="initial-review-container"
            v-on:click="$router.push({ name: 'initial' })"
          >
            <ReviewPointAbbr v-bind="reviewPointViewedByUser" />
            <InitialReviewButtons
              :key="buttonKey"
              @doInitialReview="processForm($event)"
            />
          </div>
        </template>
        <template #fullContent>
          <ShowReviewPoint
            :noteWithPosition="reviewPointViewedByUser.noteWithPosition"
            :linkViewedByUser="reviewPointViewedByUser.linkViewedByUser"
          />
          <div>
            <div class="mb-2">
              <ReviewSettingForm
                v-if="!!reviewPointViewedByUser.reviewSetting"
                v-model="reviewSetting"
                :showLevel="false"
                :errors="{}"
              />
            </div>
            <InitialReviewButtons
              :key="buttonKey"
              @doInitialReview="processForm($event)"
            />
          </div>
        </template>
      </Minimizable>
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
import Minimizable from "../components/commons/Minimizable.vue";
import storedApi from  "../managedApi/storedApi";
import storedComponent from "../store/storedComponent";

export default storedComponent({
  name: "InitialReviewPage",
  props: { nested: Boolean },
  components: {
    ShowReviewPoint,
    ReviewSettingForm,
    ContainerPage,
    InitialReviewButtons,
    Minimizable,
    ProgressBar,
    ReviewPointAbbr,
  },
  data() {
    return {
      finished: 0,
      reviewPointViewedByUser: null,
      loading: null,
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
          !(await this.$popups.confirm(
            "Are you sure to hide this note from reviewing in the future?"
          ))
        )
          return;
      }
      this.reviewPoint.removedFromReview = skipReview;
      this.storedApiExp().reviewMethods.doInitialReview(
        { reviewPoint: this.reviewPoint, reviewSetting: this.reviewSetting },
      ).then(this.loadNew)
    },

    fetchData() {
      this.storedApiExp().reviewMethods.getOneInitialReview()
      .then(this.loadNew)
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>

<style>
.initial-review-container {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>
