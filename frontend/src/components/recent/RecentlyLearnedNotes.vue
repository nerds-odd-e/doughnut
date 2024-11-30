<template>
  <div class="recently-learned-notes">
    <ContentLoader v-if="!memoryTrackers" />
    <table v-else class="table">
      <thead>
        <tr>
          <th>Note</th>
          <th>Initial Review</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="point in memoryTrackers"
            :key="point.id"
            :class="{ 'removed': point.removedFromReview }">
          <td>
            <NoteTopicWithLink :noteTopic="point.note.noteTopic" />
          </td>
          <td>{{ new Date(point.initialReviewedAt!).toLocaleString() }}</td>
          <td>{{ point.removedFromReview ? 'Removed' : 'Active' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { MemoryTracker } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteTopicWithLink from "@/components/notes/NoteTopicWithLink.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const { managedApi } = useLoadingApi()
const memoryTrackers = ref<MemoryTracker[] | undefined>(undefined)

const fetchData = async () => {
  memoryTrackers.value =
    await managedApi.restReviewPointController.getRecentReviewPoints()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.removed {
  color: gray;
  text-decoration: line-through;
}
</style>
