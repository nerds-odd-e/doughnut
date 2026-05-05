<template>
  <div class="daisy-card daisy-w-full" data-testid="folder-move-dialog">
    <div class="daisy-card-body">
      <form @submit.prevent="submit">
        <fieldset>
          <p class="daisy-text-sm daisy-mb-3">
            Move folder "{{ movingFolderName }}".
          </p>
          <label class="daisy-label" for="folder-move-destination">
            <span class="daisy-label-text">Destination</span>
          </label>
          <select
            id="folder-move-destination"
            v-model="selectedKey"
            class="daisy-select daisy-select-bordered daisy-w-full daisy-max-w-full"
            data-testid="folder-move-parent-select"
          >
            <option value="__root__">Notebook root</option>
            <option
              v-for="opt in destinationFolders"
              :key="opt.id"
              :value="String(opt.id)"
            >
              {{ opt.pathLabel }}
            </option>
          </select>
          <p v-if="loadError" class="daisy-text-error daisy-text-sm daisy-mt-2">
            {{ loadError }}
          </p>
          <p v-if="submitError" class="daisy-text-error daisy-text-sm daisy-mt-2">
            {{ submitError }}
          </p>
          <button
            type="submit"
            class="daisy-btn daisy-btn-primary daisy-mt-4"
            data-testid="folder-move-submit"
            :disabled="processing || !optionsReady"
          >
            Move folder
          </button>
        </fieldset>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FolderTrailSegment } from "@generated/doughnut-backend-api"
import { onMounted, ref } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { toOpenApiError } from "@/managedApi/openApiError"

const props = defineProps<{
  notebookId: number
  movingFolderId: number
  movingFolderName: string
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const storageAccessor = useStorageAccessor()

const processing = ref(false)
const optionsReady = ref(false)
const loadError = ref<string | undefined>(undefined)
const submitError = ref<string | undefined>(undefined)
const selectedKey = ref("__root__")
const destinationFolders = ref<{ id: number; pathLabel: string }[]>([])

function folderNumericId(folder: FolderTrailSegment): number | undefined {
  const raw = folder.id
  if (raw == null || raw === "") return undefined
  return Number(raw)
}

async function collectSubtreeFolderIds(
  rootFolderId: number
): Promise<Set<number>> {
  const ids = new Set<number>()
  const api = storageAccessor.value.storedApi()
  async function dfs(id: number) {
    ids.add(id)
    const listing = await api.loadFolderListing(props.notebookId, id)
    for (const ch of listing.folders ?? []) {
      const cid = folderNumericId(ch)
      if (cid !== undefined) await dfs(cid)
    }
  }
  await dfs(rootFolderId)
  return ids
}

async function collectAllFolderPaths(): Promise<
  { id: number; pathLabel: string }[]
> {
  const out: { id: number; pathLabel: string }[] = []
  const api = storageAccessor.value.storedApi()

  async function visitFolderTree(prefix: string, folder: FolderTrailSegment) {
    const id = folderNumericId(folder)
    if (id === undefined) return
    const name = folder.name ?? ""
    const pathLabel = prefix === "" ? name : `${prefix} / ${name}`
    out.push({ id, pathLabel })
    const childListing = await api.loadFolderListing(props.notebookId, id)
    for (const ch of childListing.folders ?? []) {
      await visitFolderTree(pathLabel, ch)
    }
  }

  const root = await api.loadNotebookRootNotes(props.notebookId)
  for (const f of root.folders ?? []) {
    await visitFolderTree("", f)
  }
  return out
}

onMounted(async () => {
  loadError.value = undefined
  try {
    const excluded = await collectSubtreeFolderIds(props.movingFolderId)
    const all = await collectAllFolderPaths()
    destinationFolders.value = all.filter((o) => !excluded.has(o.id))
    optionsReady.value = true
  } catch (e: unknown) {
    loadError.value = toOpenApiError(e).message ?? "Failed to load folders"
  }
})

const submit = async () => {
  if (processing.value || !optionsReady.value) return
  processing.value = true
  submitError.value = undefined
  try {
    const newParentFolderId =
      selectedKey.value === "__root__" ? null : Number(selectedKey.value)
    await storageAccessor.value
      .storedApi()
      .moveFolder(props.notebookId, props.movingFolderId, newParentFolderId)
    emit("closeDialog")
  } catch (e: unknown) {
    submitError.value = toOpenApiError(e).message ?? "Failed to move folder"
  } finally {
    processing.value = false
  }
}
</script>
