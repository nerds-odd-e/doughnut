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
import useLoadingApi from "@/managedApi/useLoadingApi"
import NoteTitleWithLink from "@/components/notes/NoteTitleWithLink.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"

const { managedApi } = useLoadingApi()
const memoryTrackers = ref<MemoryTracker[] | undefined>(undefined)

const fetchData = async () => {
  memoryTrackers.value = await managedApi.services.getRecentMemoryTrackers()
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
