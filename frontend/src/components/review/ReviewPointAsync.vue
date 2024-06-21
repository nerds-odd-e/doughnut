<template>
  <ContentLoader v-if="!reviewPoint" />
  <main v-else>
    <NoteWithBreadcrumb v-bind="{ note: reviewPoint.note, storageAccessor }" />
  </main>
  />
</template>

<script lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { ReviewPoint } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, defineComponent } from "vue"
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
      reviewPoint: undefined as ReviewPoint | undefined,
    }
  },
  methods: {
    async fetchData() {
      this.reviewPoint = await this.managedApi.restReviewPointController.show(
        this.reviewPointId,
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
