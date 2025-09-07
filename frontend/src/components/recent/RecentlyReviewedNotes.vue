<template>
  <div class="recently-reviewed-notes">
    <ContentLoader v-if="!memoryTrackers" />
    <table v-else class="table">
      <thead>
        <tr>
          <th>Note</th>
          <th>Initial Recall</th>
          <th>Next Recall</th>
          <th>Recall Count</th>
          <th>Curve Index</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="point in memoryTrackers"
            :key="point.id"
            :class="{ 'removed': point.removedFromTracking }">
          <td>
            <NoteTitleWithLink :noteTopology="point.note.noteTopology" />
          </td>
          <td>{{ new Date(point.assimilatedAt!).toLocaleString() }}</td>
          <td>{{ new Date(point.nextRecallAt!).toLocaleString() }}</td>
          <td>{{ point.repetitionCount }}</td>
          <td>{{ point.forgettingCurveIndex }}</td>
          <td>{{ point.removedFromTracking ? 'Removed' : 'Active' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { MemoryTracker } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteTitleWithLink from "@/components/notes/NoteTitleWithLink.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const { managedApi } = useLoadingApi()
const memoryTrackers = ref<MemoryTracker[] | undefined>(undefined)

const fetchData = async () => {
  memoryTrackers.value =
    await managedApi.restMemoryTrackerController.getRecentlyReviewed()
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
