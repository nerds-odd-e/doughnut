<template>
  <div class="daisy-card daisy-w-96">
    <div class="daisy-card-body">
      <h3 class="daisy-card-title">Export Note Data</h3>
      <button class="daisy-btn daisy-btn-primary w-full" @click="downloadDescendants">
        <SvgDownload />
        <span class="ms-2">Download All Descendants (JSON)</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { saveAs } from "file-saver"
import useLoadingApi from "@/managedApi/useLoadingApi"
import SvgDownload from "../../svgs/SvgDownload.vue"
import type { Note } from "@/generated/backend"

const props = defineProps<{ note: Note }>()
const emit = defineEmits(["close-dialog"])
const { managedApi } = useLoadingApi()

const downloadDescendants = async () => {
  const result = await managedApi.restNoteController.getDescendants(
    props.note.id
  )
  const blob = new Blob([JSON.stringify(result, null, 2)], {
    type: "application/json",
  })
  saveAs(blob, `note-${props.note.id}-descendants.json`)
  emit("close-dialog")
}
</script>
