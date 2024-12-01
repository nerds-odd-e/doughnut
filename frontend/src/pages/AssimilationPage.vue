<template>
  <ProgressBar
    v-bind="{
      paused: minimized,
      title: `Initial Review: `,
      finished,
      toRepeatCount: remainingInitialReviewCountForToday,
    }"
    @resume="resume"
  >
  </ProgressBar>
  <ContainerPage v-bind="{ contentLoaded: notes !== undefined }">
    <div v-if="notes?.length === 0" class="text-center py-8">
      You have achieved your daily new notes goal.
    </div>
    <Assimilation
      v-if="!minimized && note"
      v-bind="{ note, storageAccessor }"
      @initial-review-done="initialReviewDone"
      @reload-needed="onReloadNeeded"
      :key="note.id"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import type { Note } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import timezoneParam from "@/managedApi/window/timezoneParam"
import ProgressBar from "@/components/commons/ProgressBar.vue"
import Assimilation from "@/components/review/Assimilation.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import ContainerPage from "./commons/ContainerPage.vue"

const router = useRouter()
const { managedApi } = useLoadingApi()

defineProps({
  minimized: Boolean,
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
defineEmits(["update-reviewing"])

const finished = ref(0)
const notes = ref<Note[] | undefined>(undefined)

const note = computed(() => notes.value?.[0])
const remainingInitialReviewCountForToday = computed(
  () => notes.value?.length || 0
)
const resume = () => {
  router.push({ name: "assimilate" })
}
const initialReviewDone = () => {
  finished.value += 1
  notes.value?.shift()
}

const loadInitialReview = () => {
  managedApi.assimilationController
    .assimilating(timezoneParam())
    .then((resp) => {
      notes.value = resp
    })
}

onMounted(() => {
  loadInitialReview()
})

const onReloadNeeded = () => {
  loadInitialReview()
}
</script>
