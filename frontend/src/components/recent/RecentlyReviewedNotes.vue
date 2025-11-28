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
            :class="{ 'removed': point.removedFromTracking, 'clickable-row': true }"
            @click="navigateToMemoryTracker(point.id)">
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
import { useRouter } from "vue-router"
import type { MemoryTracker } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import NoteTitleWithLink from "@/components/notes/NoteTitleWithLink.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const router = useRouter()

const memoryTrackers = ref<MemoryTracker[] | undefined>(undefined)

const fetchData = async () => {
  const { data: trackers, error } =
    await MemoryTrackerController.getRecentlyReviewed({})
  if (!error) {
    memoryTrackers.value = trackers!
  }
}

const navigateToMemoryTracker = (memoryTrackerId: number) => {
  router.push({
    name: "memoryTrackerShow",
    params: { memoryTrackerId },
  })
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

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: rgba(0, 0, 0, 0.05);
}
</style>
