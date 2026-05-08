<template>
  <Teleport to="body">
    <dialog
      class="daisy-modal"
      :class="{ 'daisy-modal-open': open }"
      data-testid="folder-selector-search-dialog"
      style="z-index: 10050"
    >
      <div class="daisy-modal-box daisy-max-w-lg">
      <h3 class="daisy-font-bold daisy-text-lg daisy-mb-2">Find folder</h3>
      <p class="daisy-text-sm daisy-text-base-content/70 daisy-mb-1">Current selection</p>
      <p
        class="daisy-text-sm daisy-mb-4 daisy-break-words daisy-whitespace-normal daisy-max-w-full"
        data-testid="folder-selector-search-current-path"
      >
        {{ currentPathDisplay }}
      </p>
      <div class="daisy-flex daisy-items-center daisy-gap-2 daisy-w-full daisy-mb-3 daisy-input daisy-input-bordered">
        <Search class="daisy-w-4 daisy-h-4 daisy-shrink-0 daisy-opacity-50" />
        <input
          v-model="query"
          type="search"
          class="daisy-grow daisy-min-w-0 daisy-border-0 daisy-bg-transparent daisy-outline-none"
          placeholder="Search by name or path"
          data-testid="folder-selector-search-input"
          autocomplete="off"
        >
      </div>
      <ul
        class="daisy-menu daisy-bg-base-200 daisy-rounded-box daisy-max-h-64 daisy-overflow-y-auto daisy-w-full"
      >
        <li v-if="showRootRow">
          <button
            type="button"
            class="daisy-text-left daisy-w-full daisy-break-words daisy-whitespace-normal"
            data-testid="folder-selector-search-result"
            data-folder-id="__root__"
            @click="pick(null)"
          >
            Notebook root
          </button>
        </li>
        <li v-for="r in filteredFolders" :key="r.id">
          <button
            type="button"
            class="daisy-text-left daisy-w-full daisy-break-words daisy-whitespace-normal"
            data-testid="folder-selector-search-result"
            :data-folder-id="String(r.id)"
            @click="pick(r.id)"
          >
            {{ rowDisplay(r.id) }}
          </button>
        </li>
      </ul>
      <div class="daisy-modal-action daisy-mt-4">
        <button
          type="button"
          class="daisy-btn"
          data-testid="folder-selector-search-cancel"
          @click="close"
        >
          Cancel
        </button>
      </div>
    </div>
    <form method="dialog" class="daisy-modal-backdrop">
      <button type="button" @click="close">close</button>
    </form>
    </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import type { NotebookFolderIndexRow } from "@generated/doughnut-backend-api"
import { Search } from "lucide-vue-next"
import { computed, ref, watch } from "vue"
import { folderPathLabel, folderRowsById } from "./folderSelectorUtils"

const props = defineProps<{
  open: boolean
  rows: NotebookFolderIndexRow[]
  excludedFolderIds: Set<number>
  /** Line-wrapped summary for the current selection */
  currentPathDisplay: string
}>()

const emit = defineEmits<{
  close: []
  select: [folderId: number | null]
}>()

const query = ref("")

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) query.value = ""
  }
)

const byId = computed(() => folderRowsById(props.rows))

const q = computed(() => query.value.trim().toLowerCase())

const showRootRow = computed(() => {
  if (!q.value) return true
  return "notebook root".includes(q.value) || q.value === "root"
})

const filteredFolders = computed(() => {
  const out: NotebookFolderIndexRow[] = []
  for (const r of props.rows) {
    if (props.excludedFolderIds.has(r.id)) continue
    const path = folderPathLabel(r.id, byId.value).toLowerCase()
    const name = r.name.toLowerCase()
    if (!q.value || path.includes(q.value) || name.includes(q.value)) {
      out.push(r)
    }
  }
  out.sort((a, b) =>
    folderPathLabel(a.id, byId.value).localeCompare(
      folderPathLabel(b.id, byId.value)
    )
  )
  return out
})

function rowDisplay(id: number): string {
  return folderPathLabel(id, byId.value)
}

function pick(folderId: number | null) {
  emit("select", folderId)
  emit("close")
}

function close() {
  emit("close")
}
</script>
