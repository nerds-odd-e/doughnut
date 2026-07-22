<template>
  <ContentLoader v-if="folderForView === undefined" />
  <div v-else class="py-4">
    <NotebookPageReadonlySummary
      v-if="folderForView.notebookRealm.readonly === true"
      :notebook="folderForView.notebookRealm.notebook"
    />
    <div v-else class="container mx-auto max-w-6xl">
      <p class="text-sm text-base-content/70 mb-4">
        Folder
        <span class="font-semibold text-base-content">{{
          folderForView.folder.name
        }}</span>
      </p>
      <ScopedIndexNoteEditor
        :notebook-id="folderForView.notebookRealm.notebook.id"
        :folder-id="folderForView.folder.id"
        :index-content="folderForView.indexContent ?? null"
        test-id-prefix="folder-index"
        rich-editor-scope-name="folder-index"
        heading-label="Folder index"
        @saved="refreshFolderPage"
      />
      <div class="daisy-card w-full mb-6" data-testid="folder-move-dialog">
        <div class="daisy-card-body">
          <form @submit.prevent="() => submitMove()">
            <fieldset :disabled="processing">
              <p class="text-sm mb-3">
                Move folder "{{ folderForView.folder.name }}".
              </p>
              <label class="daisy-label" for="folder-move-notebook">
                <span class="daisy-label-text">Destination notebook</span>
              </label>
              <select
                id="folder-move-notebook"
                v-model="selectedDestinationNotebookId"
                class="daisy-select w-full mb-3"
                data-testid="folder-move-notebook-select"
                :disabled="processing || notebooksLoading"
              >
                <option
                  v-for="notebook in destinationNotebooks"
                  :key="notebook.id"
                  :value="notebook.id"
                >
                  {{ notebook.name }}
                </option>
              </select>
              <p
                v-if="notebooksLoadError"
                class="text-error text-sm mb-2"
              >
                {{ notebooksLoadError }}
              </p>
              <label class="daisy-label" for="folder-move-destination">
                <span class="daisy-label-text">Destination folder</span>
              </label>
              <div id="folder-move-destination">
                <FolderSelector
                  :key="folderPickerNotebookId"
                  v-model="selectedParentFolder"
                  :notebook-id="folderPickerNotebookId"
                  :context-folder="folderPickerContextFolder"
                  :ancestor-folders="folderPickerAncestorFolders"
                  :disabled="processing"
                />
              </div>
              <p v-if="moveError" class="text-error text-sm mt-2">
                {{ moveError }}
              </p>
              <button
                type="submit"
                class="daisy-btn daisy-btn-primary mt-4"
                data-testid="folder-move-submit"
                :disabled="processing"
              >
                Move folder
              </button>
            </fieldset>
          </form>
          <div class="daisy-divider my-4" />
          <form @submit.prevent="submitRename">
            <fieldset :disabled="processing">
              <p class="text-sm mb-3">
                Rename folder "{{ folderForView.folder.name }}".
              </p>
              <PathNameEditor
                v-model="renameName"
                :error-message="renameError"
                label-text="Folder name"
                editor-role="textbox"
                placeholder="Folder name"
                editor-data-test="folder-name"
              />
              <button
                type="submit"
                class="daisy-btn daisy-btn-secondary mt-4"
                data-testid="folder-rename-submit"
                :disabled="renameSubmitDisabled"
              >
                Rename folder
              </button>
            </fieldset>
          </form>
          <div class="daisy-divider my-4">or</div>
          <p class="text-sm mb-2">
            Dissolve "{{ folderForView.folder.name }}". Notes and subfolders will move to
            {{ dissolveParentLabel }}.
          </p>
          <p
            v-if="dissolveError"
            class="text-error text-sm mt-2"
          >
            {{ dissolveError }}
          </p>
          <button
            type="button"
            class="daisy-btn daisy-btn-error daisy-btn-outline"
            data-testid="folder-dissolve-button"
            :disabled="processing"
            @click="() => dissolve()"
          >
            Dissolve folder
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Folder, FolderRealm } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, onMounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import FolderSelector from "@/components/notes/FolderSelector.vue"
import { notebooksFromCatalogItems } from "@/components/notebook/notebooksFromCatalogItems"
import { sortNotebookCatalogAlphabetically } from "@/components/notebook/sortNotebookCatalogAlphabetically"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import usePopups from "@/components/commons/Popups/usePopups"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import ScopedIndexNoteEditor from "@/components/notebook/ScopedIndexNoteEditor.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"

const props = defineProps<{
  folderRealm: FolderRealm | undefined
  fetchFolderPage: () => Promise<void>
}>()

function isFolderNameConflict(
  apiError: ReturnType<typeof toOpenApiError>
): boolean {
  return apiError.errorType === "FOLDER_NAME_CONFLICT"
}

const router = useRouter()
const { popups } = usePopups()

const folderForView = computed((): FolderRealm | undefined => {
  const r = props.folderRealm
  if (r?.notebookRealm?.notebook == null) return undefined
  return r
})

const processing = ref(false)
const moveError = ref<string | undefined>(undefined)
const dissolveError = ref<string | undefined>(undefined)
const renameError = ref<string | undefined>(undefined)
const selectedParentFolder = ref<Folder | null>(null)
const renameName = ref("")
const destinationCatalogItems = ref<NotebookCatalogEntry[] | undefined>(
  undefined
)
const notebooksLoading = ref(true)
const notebooksLoadError = ref<string | undefined>(undefined)
const selectedDestinationNotebookId = ref<number | undefined>(undefined)

const sourceNotebookId = computed(
  () => folderForView.value?.notebookRealm.notebook.id
)

const destinationNotebooks = computed(() => {
  if (destinationCatalogItems.value == null) return []
  return notebooksFromCatalogItems(
    sortNotebookCatalogAlphabetically(destinationCatalogItems.value)
  )
})

const isCrossNotebookMove = computed(() => {
  const sourceId = sourceNotebookId.value
  const destinationId = selectedDestinationNotebookId.value
  return sourceId != null && destinationId != null && destinationId !== sourceId
})

const folderPickerNotebookId = computed(() => {
  if (
    isCrossNotebookMove.value &&
    selectedDestinationNotebookId.value != null
  ) {
    return selectedDestinationNotebookId.value
  }
  return folderForView.value?.notebookRealm.notebook.id ?? 0
})

const folderPickerContextFolder = computed((): Folder | null => {
  if (isCrossNotebookMove.value) return null
  return folderForView.value?.folder ?? null
})

const folderPickerAncestorFolders = computed((): Folder[] => {
  if (isCrossNotebookMove.value) return []
  return folderForView.value?.ancestorFolders ?? []
})

watch(
  sourceNotebookId,
  (notebookId) => {
    if (notebookId == null) return
    if (selectedDestinationNotebookId.value == null) {
      selectedDestinationNotebookId.value = notebookId
    }
  },
  { immediate: true }
)

watch(selectedDestinationNotebookId, (destinationId) => {
  const sourceId = sourceNotebookId.value
  if (destinationId != null && sourceId != null && destinationId !== sourceId) {
    selectedParentFolder.value = null
  }
})

onMounted(async () => {
  notebooksLoadError.value = undefined
  try {
    const { data, error } = await apiCallWithLoading(() =>
      NotebookController.myNotebooks({})
    )
    if (error || !data) throw new Error("Failed to load notebooks")
    destinationCatalogItems.value = data.catalogItems
    if (
      selectedDestinationNotebookId.value == null &&
      sourceNotebookId.value != null
    ) {
      selectedDestinationNotebookId.value = sourceNotebookId.value
    }
  } catch (e: unknown) {
    notebooksLoadError.value =
      toOpenApiError(e).message ?? "Failed to load notebooks"
  } finally {
    notebooksLoading.value = false
  }
})

watch(
  () => folderForView.value?.folder.id,
  (id) => {
    const r = folderForView.value
    if (id == null || r == null) return
    renameName.value = r.folder.name
    renameError.value = undefined
  },
  { immediate: true }
)

const renameSubmitDisabled = computed(
  () =>
    processing.value ||
    renameName.value.trim().length === 0 ||
    folderForView.value == null ||
    renameName.value.trim() === folderForView.value.folder.name
)

function dissolveParentLabelFromChain(
  movingFolderId: number,
  chain: readonly Folder[]
): string {
  const idx = chain.findIndex((f) => f.id === movingFolderId)
  if (idx <= 0) return "notebook root"
  const parentChain = chain.slice(0, idx)
  return `"${parentChain.map((f) => f.name).join(" / ")}"`
}

const dissolveParentLabel = computed(() => {
  const r = folderForView.value
  if (r == null) return "notebook root"
  return dissolveParentLabelFromChain(r.folder.id, r.ancestorFolders ?? [])
})

async function routeAfterDissolve(r: FolderRealm) {
  const notebookId = r.notebookRealm.notebook.id
  const ancestors = r.ancestorFolders ?? []
  if (ancestors.length === 0) {
    await router.push({
      name: "notebookPage",
      params: { notebookId },
    })
    return
  }
  const parent = ancestors[ancestors.length - 1]!
  await router.push({
    name: "folderPage",
    params: {
      notebookId: String(notebookId),
      folderId: String(parent.id),
    },
  })
}

const refreshFolderPage = () => props.fetchFolderPage()

function throwIfSdkError(result: {
  error?: unknown
  response?: { status?: number }
}): void {
  if (!result.error) return
  const httpStatus = result.response?.status
  const err = result.error
  if (typeof httpStatus === "number" && Number.isFinite(httpStatus)) {
    if (typeof err === "object" && err !== null) {
      throw { ...(err as Record<string, unknown>), status: httpStatus }
    }
    throw { message: String(err), status: httpStatus }
  }
  throw err
}

const submitRename = async () => {
  const r = folderForView.value
  if (processing.value || renameSubmitDisabled.value || r == null) return
  processing.value = true
  renameError.value = undefined
  try {
    const trimmed = renameName.value.trim()
    const renameResult = await apiCallWithLoading(() =>
      NotebookController.renameFolder({
        path: {
          notebook: r.notebookRealm.notebook.id,
          folder: r.folder.id,
        },
        body: { name: trimmed },
      })
    )
    throwIfSdkError(renameResult)
    refreshSidebarStructuralListings()
    await refreshFolderPage()
  } catch (e: unknown) {
    renameError.value = toOpenApiError(e).message ?? "Failed to rename folder"
  } finally {
    processing.value = false
  }
}

function buildMoveBody(merge: boolean) {
  if (isCrossNotebookMove.value) {
    const body: {
      destinationNotebookId: number | undefined
      newParentFolderId?: number
      merge: boolean
    } = {
      destinationNotebookId: selectedDestinationNotebookId.value,
      merge,
    }
    if (selectedParentFolder.value != null) {
      body.newParentFolderId = selectedParentFolder.value.id
    }
    return body
  }
  if (selectedParentFolder.value == null) {
    return { merge }
  }
  return { newParentFolderId: selectedParentFolder.value.id, merge }
}

const submitMove = async (merge = false) => {
  const r = folderForView.value
  if (processing.value || r == null) return
  processing.value = true
  moveError.value = undefined
  const destinationNotebookId =
    selectedDestinationNotebookId.value ?? r.notebookRealm.notebook.id
  try {
    const moveResult = await apiCallWithLoading(() =>
      NotebookController.moveFolder({
        path: {
          notebook: r.notebookRealm.notebook.id,
          folder: r.folder.id,
        },
        body: buildMoveBody(merge),
      })
    )
    throwIfSdkError(moveResult)
    refreshSidebarStructuralListings()
    if (merge && moveResult.data) {
      await router.push({
        name: "folderPage",
        params: {
          notebookId: String(destinationNotebookId),
          folderId: String(moveResult.data.id),
        },
      })
      return
    }
    if (isCrossNotebookMove.value) {
      await router.push({
        name: "folderPage",
        params: {
          notebookId: String(destinationNotebookId),
          folderId: String(r.folder.id),
        },
      })
      return
    }
    await refreshFolderPage()
  } catch (e: unknown) {
    const apiError = toOpenApiError(e)
    if (!merge && isFolderNameConflict(apiError)) {
      processing.value = false
      const confirmed = await popups.confirm(
        `A folder named "${r.folder.name}" already exists at the destination. Merge into it?`
      )
      if (confirmed) {
        await submitMove(true)
        return
      }
    }
    moveError.value = apiError.message ?? "Failed to move folder"
  } finally {
    processing.value = false
  }
}

const dissolve = async (merge = false) => {
  const r = folderForView.value
  if (processing.value || r == null) return
  if (!merge) {
    const ok = await popups.confirm(
      `Dissolve folder "${r.folder.name}"? Notes and subfolders will be kept.`
    )
    if (!ok) return
  }
  processing.value = true
  dissolveError.value = undefined
  try {
    const dissolveResult = await apiCallWithLoading(() =>
      NotebookController.dissolveFolder({
        path: {
          notebook: r.notebookRealm.notebook.id,
          folder: r.folder.id,
        },
        query: merge ? { merge: true } : undefined,
      })
    )
    throwIfSdkError(dissolveResult)
    refreshSidebarStructuralListings()
    await routeAfterDissolve(r)
  } catch (e: unknown) {
    const apiError = toOpenApiError(e)
    if (!merge && isFolderNameConflict(apiError)) {
      processing.value = false
      const confirmed = await popups.confirm(
        "Some subfolders share names with siblings at the destination. Merge them?"
      )
      if (confirmed) {
        await dissolve(true)
        return
      }
    }
    dissolveError.value = apiError.message ?? "Failed to dissolve folder"
  } finally {
    processing.value = false
  }
}
</script>
