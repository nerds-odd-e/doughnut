<template>
  <AssimilationPageView
    :notes="notes"
    :assimilated-count-of-the-day="assimilatedCountOfTheDay"
    :total-unassimilated-count="totalUnassimilatedCount"
    @initial-review-done="initialReviewDone"
    @reload-needed="onReloadNeeded"
  />
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
import type { Note } from "@generated/backend"
import { AssimilationController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import timezoneParam from "@/managedApi/window/timezoneParam"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import AssimilationPageView from "./AssimilationPageView.vue"

defineEmits(["update-reviewing"])

const { setDueCount, assimilatedCountOfTheDay, totalUnassimilatedCount } =
  useAssimilationCount()

const notes = ref<Note[] | undefined>(undefined)

const initialReviewDone = () => {
  notes.value?.shift()
  setDueCount(notes.value?.length)
}

const loadInitialReview = async () => {
  const { data: assimilatingNotes, error } =
    await AssimilationController.assimilating({
      query: { timezone: timezoneParam() },
    })
  if (!error) {
    notes.value = assimilatingNotes!
    setDueCount(assimilatingNotes!.length)
  }
}

onMounted(() => {
  loadInitialReview()
})

const onReloadNeeded = () => {
  loadInitialReview()
}
</script>
