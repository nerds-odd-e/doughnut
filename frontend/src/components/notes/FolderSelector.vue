<template>
  <div class="daisy-w-full">
    <p
      v-if="loadError"
      class="daisy-text-error daisy-text-sm daisy-mb-2"
    >
      {{ loadError }}
    </p>
    <div class="daisy-join daisy-w-full folder-selector-join">
      <div
        class="folder-selector-join-select daisy-join-item daisy-flex daisy-flex-1 daisy-min-w-0 daisy-items-center daisy-border daisy-border-base-content/20 daisy-bg-base-100 daisy-pl-3 daisy-pr-2 daisy-rounded-l-lg daisy-min-h-[2.75rem]"
      >
        <select
          v-model="selectModel"
          class="daisy-select daisy-select-sm daisy-w-full daisy-min-h-0 daisy-flex-1 daisy-border-0 daisy-bg-transparent daisy-shadow-none focus:daisy-outline-none focus:daisy-ring-0"
          data-testid="folder-move-parent-select"
          :disabled="disabled || rows.length === 0"
        >
        <optgroup label="Notebook root">
          <option value="__root__">
            Notebook root
          </option>
        </optgroup>
        <optgroup
          v-if="ancestorIds.length > 0"
          label="Ancestor folders"
        >
          <option
            v-for="id in ancestorIds"
            :key="`a-${id}`"
            :value="String(id)"
          >
            {{ pathForOption(id) }}
          </option>
        </optgroup>
        <optgroup
          v-if="neighbourIds.length > 0"
          label="Neighbour folders"
        >
          <option
            v-for="id in neighbourIds"
            :key="`n-${id}`"
            :value="String(id)"
          >
            {{ pathForOption(id) }}
          </option>
        </optgroup>
        <optgroup
          v-if="needsSyntheticOption && modelValue != null"
          label="Selected"
        >
          <option :value="String(modelValue)">
            {{ pathForOption(modelValue) }}
          </option>
        </optgroup>
        </select>
      </div>
      <div
        class="folder-selector-join-append daisy-join-item daisy-flex daisy-shrink-0 daisy-self-stretch daisy-items-stretch"
      >
        <button
          type="button"
          class="daisy-btn daisy-btn-outline daisy-btn-neutral daisy-rounded-l-none daisy-rounded-r-lg"
          :disabled="disabled || rows.length === 0"
          title="Search folders"
          aria-label="Search folders"
          data-testid="folder-selector-more-button"
          @click="searchOpen = true"
        >
          <MoreHorizontal class="daisy-w-5 daisy-h-5" />
        </button>
      </div>
    </div>
    <FolderSearchDialog
      :open="searchOpen"
      :rows="rows"
      :excluded-folder-ids="excludedFolderIds"
      :current-path-display="selectionSummary"
      @close="searchOpen = false"
      @select="onSearchSelect"
    />
  </div>
</template>

<script setup lang="ts">
import type { NotebookFolderIndexRow } from "@generated/doughnut-backend-api"
import { MoreHorizontal } from "lucide-vue-next"
import { computed, onMounted, ref, watch } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import FolderSearchDialog from "./FolderSearchDialog.vue"
import {
  ancestorFolderIdsOutermostFirst,
  folderPathLabel,
  folderRowsById,
  siblingFolderIds,
} from "./folderSelectorUtils"

const props = defineProps<{
  notebookId: number
  contextFolderId: number
  excludedFolderIds: Set<number>
  modelValue: number | null
  disabled?: boolean
  folderIndexRows?: NotebookFolderIndexRow[]
}>()

const emit = defineEmits<{
  "update:modelValue": [value: number | null]
}>()

const storageAccessor = useStorageAccessor()
const rows = ref<NotebookFolderIndexRow[]>(props.folderIndexRows ?? [])
const loadError = ref<string | undefined>(undefined)
const searchOpen = ref(false)

onMounted(async () => {
  if (props.folderIndexRows != null) {
    rows.value = props.folderIndexRows
    return
  }
  try {
    rows.value = await storageAccessor.value
      .storedApi()
      .loadNotebookFolderIndex(props.notebookId)
  } catch {
    loadError.value = "Failed to load folders"
  }
})

watch(
  () => props.folderIndexRows,
  (r) => {
    if (r != null) rows.value = r
  }
)

const byId = computed(() => folderRowsById(rows.value))

const ancestorIds = computed(() =>
  ancestorFolderIdsOutermostFirst(props.contextFolderId, byId.value).filter(
    (id) => !props.excludedFolderIds.has(id)
  )
)

const neighbourIds = computed(() =>
  siblingFolderIds(props.contextFolderId, rows.value, byId.value).filter(
    (id) => !props.excludedFolderIds.has(id)
  )
)

const quickPickIdSet = computed(() => {
  const s = new Set<number>()
  for (const id of ancestorIds.value) s.add(id)
  for (const id of neighbourIds.value) s.add(id)
  return s
})

const needsSyntheticOption = computed(
  () => props.modelValue != null && !quickPickIdSet.value.has(props.modelValue)
)

const selectionSummary = computed(() =>
  props.modelValue == null
    ? "Notebook root"
    : folderPathLabel(props.modelValue, byId.value)
)

const selectModel = computed({
  get(): string {
    if (props.modelValue == null) return "__root__"
    return String(props.modelValue)
  },
  set(v: string) {
    if (v === "__root__") emit("update:modelValue", null)
    else emit("update:modelValue", Number(v))
  },
})

function pathForOption(id: number): string {
  return folderPathLabel(id, byId.value)
}

function onSearchSelect(folderId: number | null) {
  emit("update:modelValue", folderId)
  searchOpen.value = false
}
</script>

<style scoped>
.folder-selector-join-append :deep(button) {
  height: 100%;
  min-height: 2.75rem;
}
</style>
