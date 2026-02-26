<template>
  <AssimilationPageView
    :key="reloadKey"
    :notes="notes"
    :assimilated-count-of-the-day="assimilatedCountOfTheDay"
    :total-unassimilated-count="totalUnassimilatedCount"
    @assimilation-done="assimilationDone"
    @reload-needed="onReloadNeeded"
  />
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import type { Note } from "@generated/backend"
import { AssimilationController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import AssimilationPageView from "./AssimilationPageView.vue"

defineEmits(["update-reviewing"])

const { setDueCount, assimilatedCountOfTheDay, totalUnassimilatedCount } =
  useAssimilationCount()

const notes = ref<Note[] | undefined>(undefined)
const reloadKey = ref(0)

const assimilationDone = () => {
  notes.value?.shift()
  setDueCount(notes.value?.length)
}

const loadAssimilation = async () => {
  const { data: assimilatingNotes, error } = await apiCallWithLoading(() =>
    AssimilationController.assimilating({
      query: { timezone: timezoneParam() },
    })
  )
  if (!error) {
    notes.value = assimilatingNotes!
    setDueCount(assimilatingNotes!.length)
  }
}

onMounted(() => {
  loadAssimilation()
})

const onReloadNeeded = async () => {
  await loadAssimilation()
  reloadKey.value += 1
}
</script>
