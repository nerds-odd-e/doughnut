<template>
  <div class="daisy-card">
    <div class="daisy-card-body">
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
          <JsonExportSection
            :json-data="jsonDescendants"
            :filename="`note-${note.id}-descendants`"
            textarea-test-id="descendants-json-textarea"
            copy-button-test-id="copy-json-btn-descendants"
            download-button-test-id="download-json-btn-descendants"
          />
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
          <div class="daisy-flex daisy-items-center daisy-gap-2 daisy-mb-2">
            <label for="token-limit" class="daisy-label-text">Token Limit:</label>
            <input
              id="token-limit"
              type="number"
              min="100"
              max="10000"
              step="100"
              v-model.number="tokenLimit"
              class="daisy-input daisy-input-sm daisy-w-24"
              data-testid="token-limit-input"
            />
            <button
              class="daisy-btn daisy-btn-ghost daisy-btn-xs"
              @click="refreshGraph"
              :disabled="loadingGraph"
              data-testid="refresh-graph-btn"
              aria-label="Refresh Graph"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24"><path stroke="currentColor" stroke-width="2" d="M4 4v5h.582M20 20v-5h-.581M19.418 9A7.994 7.994 0 0 0 12 4a8 8 0 1 0 7.418 5"/></svg>
            </button>
            <span v-if="loadingGraph" class="daisy-loading daisy-loading-spinner daisy-loading-xs"></span>
          </div>
          <JsonExportSection
            :json-data="jsonGraph"
            :filename="`note-${note.id}-graph`"
            :loading="loadingGraph"
            textarea-test-id="graph-json-textarea"
            copy-button-test-id="copy-json-btn-graph"
            download-button-test-id="download-json-btn-graph"
          />
        </div>
      </details>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import type { Note } from "@generated/backend"
import { NoteController } from "@generated/backend/sdk.gen"
import { globalClientSilent } from "@/managedApi/clientSetup"
import JsonExportSection from "../../commons/JsonExportSection.vue"

const props = defineProps<{ note: Note }>()

const expandedDescendants = ref(false)
const expandedGraph = ref(false)
const jsonDescendants = ref("")
const jsonGraph = ref("")
const tokenLimit = ref(5000)
const loadingGraph = ref(false)

watch(
  () => expandedDescendants.value,
  async (val) => {
    if (val && !jsonDescendants.value) {
      const { data: descendants, error } = await NoteController.getDescendants({
        path: { note: props.note.id },
        client: globalClientSilent,
      })
      if (!error && descendants) {
        jsonDescendants.value = JSON.stringify(descendants, null, 2)
      }
    }
  }
)

watch(
  () => expandedGraph.value,
  async (val) => {
    if (val && !jsonGraph.value) {
      await fetchGraph()
    }
  }
)

async function fetchGraph() {
  loadingGraph.value = true
  const { data: graph, error } = await NoteController.getGraph({
    path: { note: props.note.id },
    query: { tokenLimit: tokenLimit.value },
    client: globalClientSilent,
  })
  if (!error && graph) {
    jsonGraph.value = JSON.stringify(graph, null, 2)
  }
  loadingGraph.value = false
}

function refreshGraph() {
  fetchGraph()
}

function toggleExpanded(which: "descendants" | "graph", event: Event) {
  event.preventDefault()
  if (which === "descendants")
    expandedDescendants.value = !expandedDescendants.value
  if (which === "graph") expandedGraph.value = !expandedGraph.value
}
</script>
