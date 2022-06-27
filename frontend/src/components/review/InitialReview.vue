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
          <ReviewPointAbbr v-bind="{ reviewPoint }" />
        </div>
      </template>
    </ProgressBar>
    <template v-if="!nested">
      <ShowReviewPoint v-bind="{ reviewPoint }" />
      <div>
        <div class="mb-2">
          <ReviewSettingForm
            v-if="reviewPoint.thing.note?.id"
            :note-id="reviewPoint.thing.note?.id"
          />
        </div>
        <InitialReviewButtons
          :key="buttonKey"
          @do-initial-review="processForm($event)"
        />
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
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopup";
import SvgResume from "../svgs/SvgResume.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  name: "InitialReviewPage",
  props: {
    nested: Boolean,
    reviewPoints: {
      type: Object as PropType<Generated.ReviewPoint[]>,
      required: true,
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
    reviewPoint() {
      return this.reviewPoints[this.finished];
    },
    remainingInitialReviewCountForToday() {
      return this.reviewPoints.length - this.finished;
    },
    buttonKey() {
      return this.reviewPoint?.thing?.id;
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
      this.api.reviewMethods
        .doInitialReview({
          thingId: this.reviewPoint.thing.id,
          skipReview,
        })
        .then(() => {
          if (this.finished + 1 === this.reviewPoints.length) {
            this.$router.push({ name: "reviews" });
            return;
          }
          this.finished += 1;
        });
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
