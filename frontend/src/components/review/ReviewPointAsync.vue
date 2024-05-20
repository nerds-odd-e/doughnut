<template>
  <ContentLoader v-if="!reviewPoint" />
  <ShowThing
    v-else
    v-bind="{ thing: reviewPoint.thing, expandInfo: false, storageAccessor }"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { ReviewPoint } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import ContentLoader from "@/components/commons/ContentLoader.vue";
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
    ContentLoader,
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
