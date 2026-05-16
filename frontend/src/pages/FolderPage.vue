<template>
  <ContentLoader v-if="folderForView === undefined" />
  <div v-else class="daisy-py-4">
    <NotebookPageReadonlySummary
      v-if="folderForView.notebookRealm.readonly === true"
      :notebook="folderForView.notebookRealm.notebook"
    />
    <div v-else class="daisy-container daisy-mx-auto daisy-max-w-6xl">
      <p class="daisy-text-sm daisy-text-base-content/70 daisy-mb-4">
        Folder
        <span class="daisy-font-semibold daisy-text-base-content">{{
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
        save-button-idle-label="Save folder index"
        save-button-saving-label="Saving…"
        success-toast-saved="Folder index saved"
        @saved="refreshFolderPage"
      />
      <div class="daisy-card daisy-w-full daisy-mb-6" data-testid="folder-move-dialog">
        <div class="daisy-card-body">
          <form @submit.prevent="() => submitMove()">
            <fieldset :disabled="processing">
              <p class="daisy-text-sm daisy-mb-3">
                Move folder "{{ folderForView.folder.name }}".
              </p>
              <label class="daisy-label" for="folder-move-destination">
                <span class="daisy-label-text">Destination</span>
              </label>
              <div id="folder-move-destination">
                <FolderSelector
                  v-model="selectedParentFolder"
                  :notebook-id="folderForView.notebookRealm.notebook.id"
                  :context-folder="folderForView.folder"
                  :ancestor-folders="folderForView.ancestorFolders ?? []"
                  :disabled="processing"
                />
              </div>
              <p v-if="moveError" class="daisy-text-error daisy-text-sm daisy-mt-2">
                {{ moveError }}
              </p>
              <button
                type="submit"
                class="daisy-btn daisy-btn-primary daisy-mt-4"
                data-testid="folder-move-submit"
                :disabled="processing"
              >
                Move folder
              </button>
            </fieldset>
          </form>
          <div class="daisy-divider daisy-my-4" />
          <form @submit.prevent="submitRename">
            <fieldset :disabled="processing">
              <p class="daisy-text-sm daisy-mb-3">
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
                class="daisy-btn daisy-btn-secondary daisy-mt-4"
                data-testid="folder-rename-submit"
                :disabled="renameSubmitDisabled"
              >
                Rename folder
              </button>
            </fieldset>
          </form>
          <div class="daisy-divider daisy-my-4">or</div>
          <p class="daisy-text-sm daisy-mb-2">
            Dissolve "{{ folderForView.folder.name }}". Notes and subfolders will move to
            {{ dissolveParentLabel }}.
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
import { computed, ref, watch } from "vue"
import { useRouter } from "vue-router"
import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import FolderSelector from "@/components/notes/FolderSelector.vue"
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

const submitRename = async () => {
  const r = folderForView.value
  if (processing.value || renameSubmitDisabled.value || r == null) return
  processing.value = true
  renameError.value = undefined
  try {
    const trimmed = renameName.value.trim()
    const { error } = await apiCallWithLoading(() =>
      NotebookController.renameFolder({
        path: {
          notebook: r.notebookRealm.notebook.id,
          folder: r.folder.id,
        },
        body: { name: trimmed },
      })
    )
    if (error) throw error
    refreshSidebarStructuralListings()
    await refreshFolderPage()
  } catch (e: unknown) {
    renameError.value = toOpenApiError(e).message ?? "Failed to rename folder"
  } finally {
    processing.value = false
  }
}

const submitMove = async (merge = false) => {
  const r = folderForView.value
  if (processing.value || r == null) return
  processing.value = true
  moveError.value = undefined
  try {
    const body =
      selectedParentFolder.value == null
        ? { merge }
        : { newParentFolderId: selectedParentFolder.value.id, merge }
    const { error } = await apiCallWithLoading(() =>
      NotebookController.moveFolder({
        path: {
          notebook: r.notebookRealm.notebook.id,
          folder: r.folder.id,
        },
        body,
      })
    )
    if (error) throw error
    refreshSidebarStructuralListings()
    await refreshFolderPage()
  } catch (e: unknown) {
    const apiError = toOpenApiError(e)
    if (
      !merge &&
      apiError.status === 409 &&
      apiError.message === "A folder with this name already exists here."
    ) {
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
    const { error } = await apiCallWithLoading(() =>
      NotebookController.dissolveFolder({
        path: {
          notebook: r.notebookRealm.notebook.id,
          folder: r.folder.id,
        },
        query: merge ? { merge: true } : undefined,
      })
    )
    if (error) throw error
    refreshSidebarStructuralListings()
    await routeAfterDissolve(r)
  } catch (e: unknown) {
    const apiError = toOpenApiError(e)
    if (
      !merge &&
      apiError.status === 409 &&
      apiError.message?.startsWith(
        "A folder with this name already exists at the destination:"
      )
    ) {
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
