<template>
  <MemoryTrackerAsync
    v-if="memoryTrackerId"
    v-bind="{
      memoryTrackerId,
      storageAccessor,
    }"
  />
  <SelfEvaluateButtons
    @self-evaluated-memory-state="justReivew($event)"
    :key="memoryTrackerId"
  />
</template>

<script lang="ts">
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import MemoryTrackerAsync from "./MemoryTrackerAsync.vue"
import SelfEvaluateButtons from "./SelfEvaluateButtons.vue"

export default defineComponent({
  setup() {
    return { ...useLoadingApi() }
  },
  props: {
    memoryTrackerId: Number,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    MemoryTrackerAsync,
    SelfEvaluateButtons,
  },
  emits: ["reviewed"],
  methods: {
    async justReivew(successful: boolean) {
      if (this.memoryTrackerId === undefined) {
        return
      }
      await this.managedApi.services.markAsRepeated({
        memoryTracker: this.memoryTrackerId,
        successful,
      })
      this.$emit("reviewed")
    },
  },
})
</script>
