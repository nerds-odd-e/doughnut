<template>
  <ContentLoader v-if="!memoryTracker" />
  <main v-else>
    <NoteWithBreadcrumb v-bind="{ note: memoryTracker.note, storageAccessor }" />
  </main>
</template>

<script lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type { MemoryTracker } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { StorageAccessor } from "@/store/createNoteStorage"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import NoteWithBreadcrumb from "./NoteWithBreadcrumb.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
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
    NoteWithBreadcrumb,
  },
  data() {
    return {
      memoryTracker: undefined as MemoryTracker | undefined,
    }
  },
  methods: {
    async fetchData() {
      this.memoryTracker =
        await this.managedApi.restReviewPointController.show1(
          this.reviewPointId
        )
    },
  },
  watch: {
    reviewPointId() {
      this.fetchData()
    },
  },
  mounted() {
    this.fetchData()
  },
})
</script>
