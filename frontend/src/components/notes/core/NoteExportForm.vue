<template>
  <div class="daisy-card">
    <div class="daisy-card-body">
      <h3 class="daisy-card-title">Export Note Data</h3>
      <p class="daisy-text-sm daisy-text-base-content/70 daisy-mb-2">
        Focus-context markdown. The token budget limits approximate size of the focus note body plus
        all included related note bodies combined (same value for graph JSON below).
      </p>
      <div class="daisy-flex daisy-items-center daisy-gap-2 daisy-mb-2">
        <label for="context-token-limit" class="daisy-label-text">Token budget:</label>
        <input
          id="context-token-limit"
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
          type="button"
          @click="refreshMarkdown"
          :disabled="loadingMarkdown"
          data-testid="refresh-context-md-btn"
          aria-label="Refresh focus context markdown"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" viewBox="0 0 24 24"><path stroke="currentColor" stroke-width="2" d="M4 4v5h.582M20 20v-5h-.581M19.418 9A7.994 7.994 0 0 0 12 4a8 8 0 1 0 7.418 5"/></svg>
        </button>
        <span v-if="loadingMarkdown" class="daisy-loading daisy-loading-spinner daisy-loading-xs"></span>
      </div>
      <JsonExportSection
        :json-data="aiMarkdown"
        :filename="`note-${note.id}-focus-context`"
        :loading="loadingMarkdown"
        textarea-test-id="ai-context-markdown-textarea"
        copy-button-test-id="copy-ai-context-md-btn"
        download-button-test-id="download-ai-context-md-btn"
        download-mime-type="text/markdown;charset=utf-8"
        download-extension="md"
        copy-aria-label="Copy markdown"
        download-aria-label="Download markdown"
      />
      <details :open="expandedGraph" class="daisy-collapse daisy-bg-base-200 daisy-rounded-box daisy-mt-4">
        <summary
          class="daisy-flex daisy-items-center daisy-gap-2 daisy-underline daisy-cursor-pointer daisy-py-2 daisy-px-1"
          @click="toggleGraphExpanded($event)"
        >
          <svg :class="['daisy-transition-transform', 'daisy-duration-200', expandedGraph ? 'daisy-rotate-90' : '']" xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24"><path stroke="currentColor" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
          Export Note Graph (JSON)
        </summary>
        <div v-if="expandedGraph" class="daisy-mt-4">
          <p class="daisy-text-xs daisy-text-base-content/60 daisy-mb-2">
            Uses the same combined token budget as above.
          </p>
          <div class="daisy-flex daisy-items-center daisy-gap-2 daisy-mb-2">
            <button
              class="daisy-btn daisy-btn-ghost daisy-btn-xs"
              type="button"
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
import { onMounted, ref, watch } from "vue"
import type { Note } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import {} from "@/managedApi/clientSetup"
import JsonExportSection from "../../commons/JsonExportSection.vue"

const props = defineProps<{ note: Note }>()

const expandedGraph = ref(false)
const jsonGraph = ref("")
const tokenLimit = ref(2000)
const loadingGraph = ref(false)

const aiMarkdown = ref("")
const loadingMarkdown = ref(false)

onMounted(async () => {
  await fetchAiMarkdown()
})

async function fetchAiMarkdown() {
  loadingMarkdown.value = true
  const { data, error } = await NoteController.getAiContextMarkdown({
    path: { note: props.note.id },
    query: { tokenLimit: tokenLimit.value },
  })
  if (!error && data) {
    aiMarkdown.value = data.markdown ?? ""
  }
  loadingMarkdown.value = false
}

function refreshMarkdown() {
  fetchAiMarkdown()
}

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
  })
  if (!error && graph) {
    jsonGraph.value = JSON.stringify(graph, null, 2)
  }
  loadingGraph.value = false
}

function refreshGraph() {
  fetchGraph()
}

function toggleGraphExpanded(event: Event) {
  event.preventDefault()
  expandedGraph.value = !expandedGraph.value
}
</script>
