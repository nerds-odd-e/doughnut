<template>
      <h3 class="daisy-card-title">Export Note Data</h3>
      <!-- Descendants Export -->
      <details :open="expandedDescendants" class="daisy-collapse daisy-bg-base-200 daisy-rounded-box daisy-mt-4">
        <summary
          class="daisy-flex daisy-items-center daisy-gap-2 daisy-underline daisy-cursor-pointer daisy-py-2 daisy-px-1"
          @click="toggleExpanded('descendants', $event)"
        >
          <svg :class="['daisy-transition-transform', 'daisy-duration-200', expandedDescendants ? 'daisy-rotate-90' : '']" xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24"><path stroke="currentColor" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
          Export Descendants (JSON)
        </summary>
        <div v-if="expandedDescendants" class="daisy-mt-4">
          <textarea
            class="daisy-textarea daisy-textarea-bordered daisy-w-full daisy-h-48 daisy-bg-base-100 daisy-font-mono daisy-text-xs"
            readonly
            :value="jsonDescendants"
            data-testid="descendants-json-textarea"
          />
          <div class="daisy-flex daisy-gap-2 daisy-justify-end daisy-mt-2">
            <button
              class="daisy-btn daisy-btn-secondary daisy-btn-circle"
              @click="copyJson('descendants')"
              :disabled="!jsonDescendants"
              data-testid="copy-json-btn-descendants"
              aria-label="Copy JSON"
            >
              <SvgClipboard v-if="!copiedDescendants" />
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24"><path stroke="currentColor" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
            </button>
            <button
              class="daisy-btn daisy-btn-secondary daisy-btn-circle"
              @click="downloadJson('descendants')"
              :disabled="!jsonDescendants"
              data-testid="download-json-btn-descendants"
              aria-label="Download JSON"
            >
              <SvgDownload />
            </button>
          </div>
        </div>
      </details>
      <!-- Graph Export -->
      <details :open="expandedGraph" class="daisy-collapse daisy-bg-base-200 daisy-rounded-box daisy-mt-4">
        <summary
          class="daisy-flex daisy-items-center daisy-gap-2 daisy-underline daisy-cursor-pointer daisy-py-2 daisy-px-1"
          @click="toggleExpanded('graph', $event)"
        >
          <svg :class="['daisy-transition-transform', 'daisy-duration-200', expandedGraph ? 'daisy-rotate-90' : '']" xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24"><path stroke="currentColor" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
          Export Note Graph (JSON)
        </summary>
        <div v-if="expandedGraph" class="daisy-mt-4">
          <textarea
            class="daisy-textarea daisy-textarea-bordered daisy-w-full daisy-h-48 daisy-bg-base-100 daisy-font-mono daisy-text-xs"
            readonly
            :value="jsonGraph"
            data-testid="graph-json-textarea"
          />
          <div class="daisy-flex daisy-gap-2 daisy-justify-end daisy-mt-2">
            <button
              class="daisy-btn daisy-btn-secondary daisy-btn-circle"
              @click="copyJson('graph')"
              :disabled="!jsonGraph"
              data-testid="copy-json-btn-graph"
              aria-label="Copy JSON"
            >
              <SvgClipboard v-if="!copiedGraph" />
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24"><path stroke="currentColor" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
            </button>
            <button
              class="daisy-btn daisy-btn-secondary daisy-btn-circle"
              @click="downloadJson('graph')"
              :disabled="!jsonGraph"
              data-testid="download-json-btn-graph"
              aria-label="Download JSON"
            >
              <SvgDownload />
            </button>
          </div>
        </div>
      </details>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import type { Note } from "@/generated/backend"
import { saveAs } from "file-saver"
import useLoadingApi from "@/managedApi/useLoadingApi"
import SvgDownload from "../../svgs/SvgDownload.vue"
import SvgClipboard from "../../svgs/SvgClipboard.vue"

const props = defineProps<{ note: Note }>()
const { managedApi } = useLoadingApi()

const expandedDescendants = ref(false)
const expandedGraph = ref(false)
const jsonDescendants = ref("")
const jsonGraph = ref("")
const copiedDescendants = ref(false)
const copiedGraph = ref(false)

watch(
  () => expandedDescendants.value,
  async (val) => {
    if (val && !jsonDescendants.value) {
      const result = await managedApi.restNoteController.getDescendants(
        props.note.id
      )
      jsonDescendants.value = JSON.stringify(result, null, 2)
    }
  }
)

watch(
  () => expandedGraph.value,
  async (val) => {
    if (val && !jsonGraph.value) {
      const result = await managedApi.restNoteController.getGraph(props.note.id)
      jsonGraph.value = JSON.stringify(result, null, 2)
    }
  }
)

function toggleExpanded(which: "descendants" | "graph", event: Event) {
  event.preventDefault()
  if (which === "descendants")
    expandedDescendants.value = !expandedDescendants.value
  if (which === "graph") expandedGraph.value = !expandedGraph.value
}

function downloadJson(which: "descendants" | "graph") {
  const data = which === "descendants" ? jsonDescendants.value : jsonGraph.value
  if (!data) return
  const blob = new Blob([data], { type: "application/json" })
  saveAs(blob, `note-${props.note.id}-${which}.json`)
}

async function copyJson(which: "descendants" | "graph") {
  const data = which === "descendants" ? jsonDescendants.value : jsonGraph.value
  if (!data) return
  await navigator.clipboard.writeText(data)
  if (which === "descendants") {
    copiedDescendants.value = true
    setTimeout(() => {
      copiedDescendants.value = false
    }, 1200)
  } else {
    copiedGraph.value = true
    setTimeout(() => {
      copiedGraph.value = false
    }, 1200)
  }
}
</script>
