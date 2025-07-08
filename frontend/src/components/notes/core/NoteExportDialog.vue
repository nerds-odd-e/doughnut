<template>
  <div class="daisy-card">
    <div class="daisy-card-body">
      <h3 class="daisy-card-title">Export Note Data</h3>
      <details :open="expanded" class="daisy-collapse daisy-bg-base-200 daisy-rounded-box daisy-mt-4">
        <summary
          class="daisy-flex daisy-items-center daisy-gap-2 daisy-underline daisy-cursor-pointer daisy-py-2 daisy-px-1"
          @click="toggleExpanded"
        >
          <svg :class="['daisy-transition-transform', 'daisy-duration-200', expanded ? 'daisy-rotate-90' : '']" xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24"><path stroke="currentColor" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
          Export Descendants (JSON)
        </summary>
        <div v-if="expanded" class="daisy-mt-4">
          <textarea
            class="daisy-textarea daisy-textarea-bordered daisy-w-full daisy-h-48 daisy-bg-base-100 daisy-font-mono daisy-text-xs"
            readonly
            :value="jsonData"
            data-testid="descendants-json-textarea"
          />
          <button
            class="daisy-btn daisy-btn-secondary daisy-btn-circle daisy-mt-2 daisy-float-right"
            @click="downloadJson"
            :disabled="!jsonData"
            data-testid="download-json-btn"
            aria-label="Download JSON"
          >
            <SvgDownload />
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
