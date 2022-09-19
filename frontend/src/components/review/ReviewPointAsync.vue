<template>
  <LoadingPage v-bind="{ contentExists: !!reviewPoint }">
    <ShowReviewPoint
      v-if="reviewPoint"
      v-bind="{ reviewPoint, storageAccessor }"
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
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    reviewPointId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
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
