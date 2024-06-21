<template>
  <ContainerPage v-bind="{ contentExists: !!note }">
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
    <InitialReview
      v-if="!minimized && note"
      v-bind="{ note, storageAccessor }"
      @initial-review-done="initialReviewDone"
      @reload-needed="onReloadNeeded"
      :key="note.id"
    />
  </ContainerPage>
</template>

<script setup lang="ts">
import { PropType, computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import { Note } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import timezoneParam from "@/managedApi/window/timezoneParam"
import ProgressBar from "@/components/commons/ProgressBar.vue"
import InitialReview from "@/components/review/InitialReview.vue"
import { StorageAccessor } from "@/store/createNoteStorage"
import { PropType, computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
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
const notes = ref([] as Note[])

const note = computed(() => notes.value[0])
const remainingInitialReviewCountForToday = computed(() => notes.value.length)
const resume = () => {
  router.push({ name: "initial" })
}
const initialReviewDone = () => {
  finished.value += 1
  notes.value.shift()
  if (notes.value.length === 0) {
    router.push({ name: "reviews" })
    return
  }
}

const loadInitialReview = () => {
  managedApi.restReviewsController
    .initialReview(timezoneParam())
    .then((resp) => {
      if (resp.length === 0) {
        router.push({ name: "reviews" })
        return
      }
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
