<template>
  <div class="recently-learned-notes">
    <ContentLoader v-if="!reviewPoints" />
    <table v-else class="table">
      <thead>
        <tr>
          <th>Note</th>
          <th>Initial Review</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="point in reviewPoints"
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
import type { ReviewPoint } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteTopicWithLink from "@/components/notes/NoteTopicWithLink.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const { managedApi } = useLoadingApi()
const reviewPoints = ref<ReviewPoint[] | undefined>(undefined)

const fetchData = async () => {
  reviewPoints.value =
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
