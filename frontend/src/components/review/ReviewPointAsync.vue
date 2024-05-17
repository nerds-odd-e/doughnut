<template>
  <LoadingPage v-bind="{ contentExists: !!reviewPoint }">
    <ShowThing
      v-if="reviewPoint"
      v-bind="{ thing: reviewPoint.thing, expandInfo: false, storageAccessor }"
    />
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { ReviewPoint } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import ShowThing from "./ShowThing.vue";

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
    ShowThing,
  },
  data() {
    return {
      reviewPoint: undefined as ReviewPoint | undefined,
    };
  },
  methods: {
    async fetchData() {
      this.reviewPoint = await this.managedApi.restReviewPointController.show(
        this.reviewPointId,
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
