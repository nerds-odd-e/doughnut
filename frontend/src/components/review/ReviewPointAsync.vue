<template>
  <LoadingPage v-bind="{ loading, contentExists: !!reviewPoint }">
    <ShowReviewPoint
      v-if="reviewPoint"
      v-bind="{ reviewPoint, historyWriter }"
      :key="reviewPointId"
      @self-evaluated="onSelfEvaluted($event)"
    />
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import ShowReviewPoint from "./ShowReviewPoint.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true });
  },
  props: {
    reviewPointId: { type: Number, required: true },
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
    },
  },
  components: {
    LoadingPage,
    ShowReviewPoint,
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
