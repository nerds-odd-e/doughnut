<template>
  <div class="daisy-card daisy-w-96">
    <div class="daisy-card-body">
      <h3 class="daisy-card-title">Export Note Data</h3>
      <details :open="expanded">
        <summary class="daisy-btn daisy-btn-primary w-full" @click="toggleExpanded">Export Descendants (JSON)</summary>
        <div v-if="expanded" class="mt-4">
          <textarea
            class="daisy-textarea w-full h-48"
            readonly
            :value="jsonData"
            data-testid="descendants-json-textarea"
          />
          <button
            class="daisy-btn daisy-btn-secondary w-full mt-2"
            @click="downloadJson"
            :disabled="!jsonData"
            data-testid="download-json-btn"
          >
            <SvgDownload />
            <span class="ms-2">Download JSON</span>
          </button>
        </div>
      </details>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import type { Note } from "@/generated/backend"
import { saveAs } from "file-saver"
import useLoadingApi from "@/managedApi/useLoadingApi"
import SvgDownload from "../../svgs/SvgDownload.vue"

const props = defineProps<{ note: Note }>()
const { managedApi } = useLoadingApi()

const expanded = ref(false)
const jsonData = ref("")

watch(
  () => expanded.value,
  async (val) => {
    if (val && !jsonData.value) {
      const result = await managedApi.restNoteController.getDescendants(
        props.note.id
      )
      jsonData.value = JSON.stringify(result, null, 2)
    }
  }
)

function toggleExpanded(event: Event) {
  event.preventDefault()
  expanded.value = !expanded.value
}

function downloadJson() {
  if (!jsonData.value) return
  const blob = new Blob([jsonData.value], { type: "application/json" })
  saveAs(blob, `note-${props.note.id}-descendants.json`)
}
</script>
