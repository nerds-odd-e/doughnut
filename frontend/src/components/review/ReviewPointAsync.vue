<template>
  <LoadingPage v-bind="{ loading, contentExists: !!reviewPoint }">
    <ShowReviewPoint
      v-if="reviewPoint"
      v-bind="{ reviewPoint }"
      :key="reviewPointId"
    />
    <NoteInfoReviewPoint
      v-if="reviewPoint"
      v-bind="{ reviewPoint }"
      @self-evaluated="onSelfEvaluted($event)"
    />
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import ShowReviewPoint from "./ShowReviewPoint.vue";
import NoteInfoReviewPoint from "../notes/NoteInfoReviewPoint.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true });
  },
  props: {
    reviewPointId: { type: Number, required: true },
  },
  components: {
    LoadingPage,
    ShowReviewPoint,
    NoteInfoReviewPoint,
  },
  data() {
    return {
      reviewPoint: undefined as Generated.ReviewPoint | undefined,
    };
  },
  methods: {
    onSelfEvaluted(reviewPoint: Generated.ReviewPoint) {
      this.reviewPoint = reviewPoint;
    },
    async fetchData() {
      this.reviewPoint = await this.api.reviewMethods.getReviewPoint(
        this.reviewPointId
      );
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
