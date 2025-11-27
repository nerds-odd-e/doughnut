<template>
  <div class="recently-learned-notes">
    <ContentLoader v-if="!memoryTrackers" />
    <table v-else class="table">
      <thead>
        <tr>
          <th>Note</th>
          <th>Assimilated</th>
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
          <td>{{ point.removedFromTracking ? 'Removed' : 'Active' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { MemoryTracker } from "@generated/backend"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"
import NoteTitleWithLink from "@/components/notes/NoteTitleWithLink.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const memoryTrackers = ref<MemoryTracker[] | undefined>(undefined)

const fetchData = async () => {
  const { data: trackers, error } =
    await MemoryTrackerController.getRecentMemoryTrackers()
  if (!error) {
    memoryTrackers.value = trackers!
  }
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
