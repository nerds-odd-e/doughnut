<template>
  <ReviewPointAsync
    v-if="reviewPointId"
    v-bind="{
      reviewPointId,
      storageAccessor,
    }"
  />
  <SelfEvaluateButtons
    @self-evaluated-memory-state="justReivew($event)"
    :key="reviewPointId"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ReviewPointAsync from "./ReviewPointAsync.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    reviewPointId: Number,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    ReviewPointAsync,
    SelfEvaluateButtons,
  },
  emits: ["reviewed"],
  methods: {
    async justReivew(successful: boolean) {
      if (this.reviewPointId === undefined) {
        return;
      }
      await this.api.reviewMethods.markAsRepeated(
        this.reviewPointId,
        successful,
      );
      this.$emit("reviewed");
    },
  },
});
</script>
