<template>
  <div class="daisy-card daisy-w-full" data-testid="folder-move-dialog">
    <div class="daisy-card-body">
      <form @submit.prevent="submitMove">
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
          <p v-if="moveError" class="daisy-text-error daisy-text-sm daisy-mt-2">
            {{ moveError }}
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
      <div class="daisy-divider daisy-my-4">or</div>
      <p class="daisy-text-sm daisy-mb-2">
        Dissolve "{{ movingFolderName }}". Notes and subfolders will move to
        {{ parentLocationLabel }}.
      </p>
      <p
        v-if="dissolveError"
        class="daisy-text-error daisy-text-sm daisy-mt-2"
      >
        {{ dissolveError }}
      </p>
      <button
        type="button"
        class="daisy-btn daisy-btn-error daisy-btn-outline"
        data-testid="folder-dissolve-button"
        :disabled="processing"
        @click="dissolve"
      >
        Dissolve folder
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Folder } from "@generated/doughnut-backend-api"
import { onMounted, ref } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { toOpenApiError } from "@/managedApi/openApiError"
import usePopups from "../commons/Popups/usePopups"
import { notebookSidebarUserActiveFolder } from "@/composables/useCurrentNoteSidebarState"

const props = defineProps<{
  notebookId: number
  movingFolderId: number
  movingFolderName: string
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const storageAccessor = useStorageAccessor()
const { popups } = usePopups()

const processing = ref(false)
const optionsReady = ref(false)
const loadError = ref<string | undefined>(undefined)
const moveError = ref<string | undefined>(undefined)
const dissolveError = ref<string | undefined>(undefined)
const selectedKey = ref("__root__")
const destinationFolders = ref<{ id: number; pathLabel: string }[]>([])
const parentLocationLabel = ref("notebook root")

function folderNumericId(folder: Folder): number | undefined {
  return folder.id
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

  async function visitFolderTree(prefix: string, folder: Folder) {
    const id = folderNumericId(folder)
    if (id === undefined) return
    const name = folder.name
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

function findParentLabel(
  all: { id: number; pathLabel: string }[],
  movingFolderId: number
): string {
  const moving = all.find((o) => o.id === movingFolderId)
  if (!moving) return "notebook root"
  const segments = moving.pathLabel.split(" / ")
  if (segments.length <= 1) return "notebook root"
  return `"${segments.slice(0, -1).join(" / ")}"`
}

onMounted(async () => {
  loadError.value = undefined
  try {
    const excluded = await collectSubtreeFolderIds(props.movingFolderId)
    const all = await collectAllFolderPaths()
    destinationFolders.value = all.filter((o) => !excluded.has(o.id))
    parentLocationLabel.value = findParentLabel(all, props.movingFolderId)
    optionsReady.value = true
  } catch (e: unknown) {
    loadError.value = toOpenApiError(e).message ?? "Failed to load folders"
  }
})

const submitMove = async () => {
  if (processing.value || !optionsReady.value) return
  processing.value = true
  moveError.value = undefined
  try {
    const newParentFolderId =
      selectedKey.value === "__root__" ? null : Number(selectedKey.value)
    await storageAccessor.value
      .storedApi()
      .moveFolder(props.notebookId, props.movingFolderId, newParentFolderId)
    emit("closeDialog")
  } catch (e: unknown) {
    moveError.value = toOpenApiError(e).message ?? "Failed to move folder"
  } finally {
    processing.value = false
  }
}

const dissolve = async () => {
  if (processing.value) return
  const ok = await popups.confirm(
    `Dissolve folder "${props.movingFolderName}"? Notes and subfolders will be kept.`
  )
  if (!ok) return
  processing.value = true
  dissolveError.value = undefined
  try {
    await storageAccessor.value
      .storedApi()
      .dissolveFolder(props.notebookId, props.movingFolderId)
    notebookSidebarUserActiveFolder.value = null
    emit("closeDialog")
  } catch (e: unknown) {
    dissolveError.value =
      toOpenApiError(e).message ?? "Failed to dissolve folder"
  } finally {
    processing.value = false
  }
}
</script>
