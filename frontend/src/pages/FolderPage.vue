<template>
  <ContentLoader v-if="folderForView === undefined" />
  <div v-else class="daisy-py-4">
    <NotebookPageReadonlySummary
      v-if="folderForView.notebookView.readonly === true"
      :notebook="folderForView.notebookView.notebook"
    />
    <div v-else class="daisy-container daisy-mx-auto daisy-max-w-6xl">
      <p class="daisy-text-sm daisy-text-base-content/70 daisy-mb-4">
        Folder
        <span class="daisy-font-semibold daisy-text-base-content">{{
          folderForView.folder.name
        }}</span>
      </p>
      <ScopedIndexNoteEditor
        :notebook-id="folderForView.notebookView.notebook.id"
        :folder-id="folderForView.folder.id"
        :index-note-status="indexNoteStatus"
        :index-note-id="sidebarAnchorNoteId"
        :fetch-page="fetchFolderPage"
        test-id-prefix="folder-index"
        rich-editor-scope-name="folder-index"
        loading-hint="Loading folder index…"
        absent-heading="Folder index"
        absent-help-before="No index note yet. Edit below and save to create a note titled "
        absent-help-after=" in this folder."
        present-title-fallback="Folder index"
        save-button-idle-label="Save folder index"
        save-button-saving-label="Saving…"
        success-toast-saved="Folder index saved"
        success-toast-after-race="Folder index is now available"
        error-toast-after-race="Could not create folder index: a conflicting note may exist. Refresh the page and try again."
        @index-note-created="fetchFolderPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRoute } from "vue-router"
import type {
  FolderRealm,
  NotebookRealm,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookPageReadonlySummary from "@/components/notebook/NotebookPageReadonlySummary.vue"
import ScopedIndexNoteEditor from "@/components/notebook/ScopedIndexNoteEditor.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import {
  currentActiveNoteId,
  folderPageBreadcrumbFolders,
  folderSidebarFolderRealm,
  notebookSidebarNotebookRealm,
  notebookSidebarActiveFolder,
} from "@/composables/useCurrentNoteSidebarState"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { folderBreadcrumbChainFromFlatIndex } from "@/utils/folderBreadcrumbChain"

const route = useRoute()
const storageAccessor = useStorageAccessor()

const folderRealm = ref<FolderRealm | undefined>(undefined)

const folderForView = computed((): FolderRealm | undefined => {
  const r = folderRealm.value
  if (r?.notebookView?.notebook == null) return undefined
  return r
})

const sidebarAnchorNoteId = ref<number | undefined>()
const indexNoteStatus = ref<"pending" | "present" | "absent">("pending")
let indexResolveGeneration = 0

function notebookChromeFromFolderRealm(c: FolderRealm): NotebookRealm {
  return { ...c.notebookView }
}

const fetchFolderPage = async () => {
  const notebookId = Number(route.params.notebookId)
  const folderId = Number(route.params.folderId)
  const { data: page, error } = await NotebookController.getFolderPage({
    path: { notebook: notebookId, folder: folderId },
  })
  if (!error && page?.notebookView?.notebook) {
    folderRealm.value = page
    const { data: indexRows, error: indexErr } =
      await NotebookController.listNotebookFolderIndex({
        path: { notebook: notebookId },
      })
    if (!indexErr && indexRows) {
      folderPageBreadcrumbFolders.value = folderBreadcrumbChainFromFlatIndex(
        page.folder,
        indexRows
      )
    } else {
      folderPageBreadcrumbFolders.value = [page.folder]
    }
    notebookSidebarActiveFolder.value = page
    return
  }
  folderRealm.value = undefined
  folderPageBreadcrumbFolders.value = []
}

watch(
  folderRealm,
  (c) => {
    folderSidebarFolderRealm.value = c
    const chromeNotebookId = c?.notebookView?.notebook?.id
    if (chromeNotebookId == null) {
      notebookSidebarNotebookRealm.value = undefined
      return
    }
    notebookSidebarNotebookRealm.value = notebookChromeFromFolderRealm(
      c as FolderRealm
    )
  },
  { immediate: true, deep: true }
)

watch(
  () =>
    folderRealm.value?.folder
      ? ([
          folderRealm.value.folder.id,
          folderRealm.value.folderIndexNoteId ?? null,
        ] as const)
      : undefined,
  async (key) => {
    if (key === undefined) {
      sidebarAnchorNoteId.value = undefined
      indexNoteStatus.value = "pending"
      currentActiveNoteId.value = undefined
      return
    }

    const [, folderIndexNoteId] = key
    const gen = ++indexResolveGeneration
    sidebarAnchorNoteId.value = undefined
    indexNoteStatus.value = "pending"

    if (folderIndexNoteId == null) {
      indexNoteStatus.value = "absent"
      return
    }

    try {
      await storageAccessor.value.storedApi().loadNoteRealm(folderIndexNoteId)
      if (gen !== indexResolveGeneration) return
      sidebarAnchorNoteId.value = folderIndexNoteId
      indexNoteStatus.value = "present"
    } catch {
      if (gen !== indexResolveGeneration) return
      indexNoteStatus.value = "absent"
    }
  },
  { immediate: true }
)

watch(
  sidebarAnchorNoteId,
  (id) => {
    currentActiveNoteId.value = id
  },
  { immediate: true }
)

watch(
  () => [route.params.notebookId, route.params.folderId] as const,
  async () => {
    await fetchFolderPage()
  }
)

onMounted(async () => {
  await fetchFolderPage()
})

onBeforeUnmount(() => {
  notebookSidebarNotebookRealm.value = undefined
  folderSidebarFolderRealm.value = undefined
  folderPageBreadcrumbFolders.value = []
})
</script>
