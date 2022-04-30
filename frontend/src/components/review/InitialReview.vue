<template>
  <ContainerPage v-bind="{ loading, contentExists: true }">
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
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ShowReviewPoint from "./ShowReviewPoint.vue";
import ReviewSettingForm from "./ReviewSettingForm.vue";
import ReviewPointAbbr from "./ReviewPointAbbr.vue";
import InitialReviewButtons from "./InitialReviewButtons.vue";
import ProgressBar from "../commons/ProgressBar.vue";
import ContainerPage from "../../pages/commons/ContainerPage.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import usePopups from "../commons/Popups/usePopup";
import SvgResume from "../svgs/SvgResume.vue";

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi(), ...usePopups() };
  },
  name: "InitialReviewPage",
  props: {
     nested: Boolean,
     reviewPointViewedByUsers: {
        type: Object as PropType<Generated.ReviewPointViewedByUser[]>,
        required: true
     },
   },
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
    };
  },
  computed: {
    reviewPointViewedByUser() {
      return this.reviewPointViewedByUsers[this.finished];
    },
    reviewPoint() {
      return this.reviewPointViewedByUser?.reviewPoint;
    },
    reviewSetting() {
      return this.reviewPointViewedByUser?.reviewSetting;
    },
    remainingInitialReviewCountForToday() {
      return this.reviewPointViewedByUsers.length - this.finished;
    },
    buttonKey() {
      return !!this.reviewPoint?.noteId
        ? `note-${this.reviewPoint.noteId}`
        : `link-${this.reviewPoint?.linkId}`;
    },
  },

  methods: {
    resume() {
      this.$router.push({ name: "initial" });
    },

    async processForm(skipReview: boolean) {
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
          noteId: this.reviewPoint.noteId,
          linkId: this.reviewPoint.linkId,
          reviewSetting: this.reviewSetting,
        })
        .then((res)=>{
          this.reviewPointViewedByUsers[this.finished] = res;
          if (this.finished + 1 === this.reviewPointViewedByUsers.length) {
            this.$router.push({ name: "reviews" });
            return
          }
          this.finished += 1;
        })
    },
  },
});
</script>

<style>
.initial-review-paused {
  background-color: rgba(50, 50, 150, 0.8);
  padding: 5px;
  border-radius: 10px;
}
</style>
