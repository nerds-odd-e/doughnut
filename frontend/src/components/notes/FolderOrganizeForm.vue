<template>
  <div class="daisy-card daisy-w-full" data-testid="folder-move-dialog">
    <div class="daisy-card-body">
      <form @submit.prevent="submitMove">
        <fieldset :disabled="processing">
          <p class="daisy-text-sm daisy-mb-3">
            Move folder "{{ movingFolderRealm.folder.name }}".
          </p>
          <label class="daisy-label" for="folder-move-destination">
            <span class="daisy-label-text">Destination</span>
          </label>
          <div id="folder-move-destination">
            <FolderSelector
              v-model="selectedParentFolder"
              :notebook-id="movingFolderRealm.notebookRealm.notebook.id"
              :context-folder="movingFolderRealm.folder"
              :ancestor-folders="movingFolderRealm.ancestorFolders ?? []"
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
            Rename folder "{{ movingFolderRealm.folder.name }}".
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
        Dissolve "{{ movingFolderRealm.folder.name }}". Notes and subfolders will move to
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
        @click="dissolve"
      >
        Dissolve folder
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Folder, FolderRealm } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, inject, ref, watch } from "vue"
import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import usePopups from "../commons/Popups/usePopups"
import FolderSelector from "./FolderSelector.vue"

const props = defineProps<{
  movingFolderRealm: FolderRealm
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const { popups } = usePopups()

const reloadFolderPage = inject<(() => Promise<void>) | undefined>(
  "reloadFolderPage",
  undefined
)

const processing = ref(false)
const moveError = ref<string | undefined>(undefined)
const dissolveError = ref<string | undefined>(undefined)
const renameError = ref<string | undefined>(undefined)
const selectedParentFolder = ref<Folder | null>(null)
const renameName = ref(props.movingFolderRealm.folder.name)

watch(
  () => props.movingFolderRealm.folder.id,
  () => {
    renameName.value = props.movingFolderRealm.folder.name
    renameError.value = undefined
  }
)

const renameSubmitDisabled = computed(
  () =>
    processing.value ||
    renameName.value.trim().length === 0 ||
    renameName.value.trim() === props.movingFolderRealm.folder.name
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

const dissolveParentLabel = computed(() =>
  dissolveParentLabelFromChain(
    props.movingFolderRealm.folder.id,
    props.movingFolderRealm.ancestorFolders ?? []
  )
)

const submitRename = async () => {
  if (processing.value || renameSubmitDisabled.value) return
  processing.value = true
  renameError.value = undefined
  try {
    const trimmed = renameName.value.trim()
    const { error } = await apiCallWithLoading(() =>
      NotebookController.renameFolder({
        path: {
          notebook: props.movingFolderRealm.notebookRealm.notebook.id,
          folder: props.movingFolderRealm.folder.id,
        },
        body: { name: trimmed },
      })
    )
    if (error) throw error
    refreshSidebarStructuralListings()
    await reloadFolderPage?.()
    emit("closeDialog")
  } catch (e: unknown) {
    renameError.value = toOpenApiError(e).message ?? "Failed to rename folder"
  } finally {
    processing.value = false
  }
}

const submitMove = async () => {
  if (processing.value) return
  processing.value = true
  moveError.value = undefined
  try {
    const body =
      selectedParentFolder.value == null
        ? {}
        : { newParentFolderId: selectedParentFolder.value.id }
    const { error } = await apiCallWithLoading(() =>
      NotebookController.moveFolder({
        path: {
          notebook: props.movingFolderRealm.notebookRealm.notebook.id,
          folder: props.movingFolderRealm.folder.id,
        },
        body,
      })
    )
    if (error) throw error
    refreshSidebarStructuralListings()
    await reloadFolderPage?.()
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
    `Dissolve folder "${props.movingFolderRealm.folder.name}"? Notes and subfolders will be kept.`
  )
  if (!ok) return
  processing.value = true
  dissolveError.value = undefined
  try {
    const { error } = await apiCallWithLoading(() =>
      NotebookController.dissolveFolder({
        path: {
          notebook: props.movingFolderRealm.notebookRealm.notebook.id,
          folder: props.movingFolderRealm.folder.id,
        },
      })
    )
    if (error) throw error
    refreshSidebarStructuralListings()
    emit("closeDialog")
  } catch (e: unknown) {
    dissolveError.value =
      toOpenApiError(e).message ?? "Failed to dissolve folder"
  } finally {
    processing.value = false
  }
}
</script>
