<template>
  <div class="recently-added-notes">
    <ContentLoader v-if="!notes" />
    <div v-else class="result-section">
      <div class="result-title">Recently updated notes</div>
      <Cards
        class="search-result"
        :noteTopologies="notes"
        :columns="3"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { NoteTopology } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import Cards from "@/components/notes/Cards.vue"

const notes = ref<NoteTopology[] | undefined>(undefined)

const fetchData = async () => {
  const { data: recentNotes, error } = await NoteController.getRecentNotes({})
  if (!error) {
    notes.value = recentNotes!
  }
}

onMounted(() => {
  fetchData()
})
</script>
