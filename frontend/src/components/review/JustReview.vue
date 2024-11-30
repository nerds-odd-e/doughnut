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
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import ReviewPointAsync from "./ReviewPointAsync.vue"
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue"

export default defineComponent({
  setup() {
    return { ...useLoadingApi() }
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
        return
      }
      await this.managedApi.restMemoryTrackerController.markAsRepeated(
        this.reviewPointId,
        successful
      )
      this.$emit("reviewed")
    },
  },
})
</script>
