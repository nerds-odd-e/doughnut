<template>
  <div class="daisy-card daisy-w-full">
    <div class="daisy-card-body">
      <form data-testid="note-new-form" @submit.prevent="processForm">
        <fieldset :disabled="processing">
          <div class="daisy-mb-4">
            <p class="daisy-text-sm daisy-mb-2">
              Folder
            </p>
            <FolderSelector
              v-model="selectedFolderId"
              :notebook-id="notebookRootNotebookId"
              :context-folder-id="folderSelectorContextFolderId"
              :ancestor-folders="ancestorFolders"
              :model-value-label="folderSelectorLabel"
              :disabled="processing"
            />
          </div>
          <div class="title-search-container">
            <PathNameEditor
              v-model="newTitle"
              :error-message="noteFormErrors.newTitle"
              autofocus
              initial-select-all
              @update:model-value="onTitleChange"
            >
              <template #append>
                <WikidataSearchByLabel
                  :search-key="newTitle"
                  v-model="wikidataIdSelection"
                  :error-message="noteFormErrors.wikidataId"
                  @selected="onSelectWikidataEntry"
                />
              </template>
            </PathNameEditor>
            <SearchResults
              v-bind="{
                noteId: titleSearchScopeNote?.id,
                inputSearchKey: effectiveSearchKey,
                isDropdown: true,
                notebookId: notebookRootNotebookId,
              }"
              class="title-search-results"
            />
          </div>
          <input
            type="submit"
            value="Submit"
            class="daisy-btn daisy-btn-primary daisy-mt-4"
          />
        </fieldset>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  Folder,
  WikidataSearchEntity,
  Note,
  NoteCreationDto,
} from "@generated/doughnut-backend-api"
import { ref, computed, watch } from "vue"
import SearchResults from "../search/SearchResults.vue"
import FolderSelector from "./FolderSelector.vue"
import PathNameEditor from "./core/PathNameEditor.vue"
import WikidataSearchByLabel from "./WikidataSearchByLabel.vue"
import { useRouter } from "vue-router"
import { calculateNewTitle } from "@/utils/wikidataTitleActions"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { toOpenApiError } from "@/managedApi/openApiError"
import usePopups from "@/components/commons/Popups/usePopups"
import type { SidebarActiveFolder } from "./useNoteSidebarTree"

const router = useRouter()
const storageAccessor = useStorageAccessor()
const { popups } = usePopups()

const props = withDefaults(
  defineProps<{
    notebookRootNotebookId: number
    /** Initial folder scope for create-note (sidebar selection or active note folder). User can change via FolderSelector. */
    initialFolder?: SidebarActiveFolder
    parentLocationDescription?: string
    initialTitle?: string
    /**
     * Default title from scoped index `title_pattern` (already rendered). Does not mark the title
     * as user-edited; ignored when `initialTitle` is set.
     */
    defaultTitleFromScopedPattern?: string
    /**
     * When set, search is scoped under this note; when omitted (e.g. notebook-level new note), search
     * is global and the dropdown still prioritizes hits in `notebookRootNotebookId`.
     */
    titleSearchAnchorNote?: Note
    /** After notebook-root create, refresh wiki title cache for this note before navigating away. */
    wikiTitleCacheRefreshSourceNoteId?: number
    /** Root-to-leaf ancestor chain for FolderSelector (same as NoteRealm.ancestorFolders). */
    ancestorFolders?: Folder[]
  }>(),
  { ancestorFolders: () => [] }
)

const titleSearchScopeNote = computed(() => props.titleSearchAnchorNote)

const selectedFolderId = ref<number | null>(props.initialFolder?.id ?? null)

watch(
  () => props.initialFolder?.id,
  (v) => {
    selectedFolderId.value = v ?? null
  }
)

/** Fallback label for the folder selector when the path can't be resolved from ancestorFolders. */
const folderSelectorLabel = computed((): string | undefined => {
  const id = selectedFolderId.value ?? props.initialFolder?.id ?? null
  if (id == null) return undefined
  const found = props.ancestorFolders.find((f) => f.id === id)
  return found?.name ?? props.initialFolder?.name
})

/** Context folder for FolderSelector quick picks; null at notebook root. */
const folderSelectorContextFolderId = computed(
  (): number | null => selectedFolderId.value ?? props.initialFolder?.id ?? null
)

// Emits
const emit = defineEmits<{
  closeDialog: []
}>()

function contentWithWikidataFrontmatter(
  wikidataId: string
): string | undefined {
  const t = wikidataId.trim()
  if (!t) return undefined
  return `---\nwikidata_id: ${t}\n---\n`
}

// Reactive state
const newTitle = ref(
  props.initialTitle ?? props.defaultTitleFromScopedPattern ?? "Untitled"
)
const wikidataIdSelection = ref("")

const noteFormErrors = ref({
  newTitle: undefined as undefined | string,
  wikidataId: undefined as undefined | string,
})

const processing = ref(false)
const hasTitleBeenEdited = ref(props.initialTitle !== undefined)

watch(
  () => [props.defaultTitleFromScopedPattern, props.initialTitle] as const,
  ([def, init]) => {
    if (init !== undefined) return
    if (!hasTitleBeenEdited.value) {
      newTitle.value = def ?? "Untitled"
    }
  }
)

// Computed property to determine effective search key
const effectiveSearchKey = computed(() => {
  // Don't search if title hasn't been edited yet
  if (!hasTitleBeenEdited.value) {
    return ""
  }
  return newTitle.value
})

// Methods
function parseCreateNoteFailure(e: unknown): {
  fieldErrors: { newTitle?: string; wikidataId?: string }
  softDeletedNoteId?: number
} {
  const err = e as { body?: unknown }
  const body = err.body
  if (body && typeof body === "object") {
    const parsed = toOpenApiError(body)
    const errorType =
      "errorType" in body &&
      typeof (body as { errorType: unknown }).errorType === "string"
        ? (body as { errorType: string }).errorType
        : parsed.errorType
    if (
      errorType === "SOFT_DELETED_TITLE_CONFLICT" &&
      parsed.errors?.deletedNoteId
    ) {
      const id = Number(parsed.errors.deletedNoteId)
      if (!Number.isNaN(id)) {
        return { fieldErrors: {}, softDeletedNoteId: id }
      }
    }
  }
  return {
    fieldErrors: {
      newTitle: undefined,
      wikidataId: undefined,
      ...(typeof e === "object" && e !== null ? (e as object) : {}),
    },
  }
}

const processForm = async () => {
  if (processing.value) return
  processing.value = true
  noteFormErrors.value.wikidataId = undefined
  noteFormErrors.value.newTitle = undefined

  const trimmedWikidata = wikidataIdSelection.value.trim()
  if (trimmedWikidata !== "" && !/^Q\d+$/i.test(trimmedWikidata)) {
    noteFormErrors.value.wikidataId = "The wikidata Id should be Q<numbers>"
    processing.value = false
    return
  }

  const api = storageAccessor.value.storedApi()
  const wikidataContent = contentWithWikidataFrontmatter(
    wikidataIdSelection.value
  )
  const body: NoteCreationDto = {
    newTitle: newTitle.value,
    ...(wikidataContent !== undefined ? { content: wikidataContent } : {}),
  }
  try {
    await api.createRootNoteAtNotebook(
      router,
      props.notebookRootNotebookId,
      body,
      {
        folderId: selectedFolderId.value ?? undefined,
        refreshWikiTitleCacheForNoteIds:
          props.wikiTitleCacheRefreshSourceNoteId != null
            ? [props.wikiTitleCacheRefreshSourceNoteId]
            : undefined,
      }
    )
    emit("closeDialog")
  } catch (e: unknown) {
    const { fieldErrors, softDeletedNoteId } = parseCreateNoteFailure(e)
    if (softDeletedNoteId != null) {
      const confirmed = await popups.confirm(
        "A note with this title was deleted. OK restores that note instead of creating a new one."
      )
      if (confirmed) {
        try {
          await storageAccessor.value
            .storedApi()
            .restoreDeletedNote(router, softDeletedNoteId)
          emit("closeDialog")
        } catch (res: unknown) {
          noteFormErrors.value = {
            newTitle: undefined,
            wikidataId: undefined,
            ...(res as object),
          }
        }
      }
    } else {
      noteFormErrors.value = {
        newTitle: fieldErrors.newTitle,
        wikidataId: fieldErrors.wikidataId,
      }
    }
  } finally {
    processing.value = false
  }
}

const onSelectWikidataEntry = (
  selectedSuggestion: WikidataSearchEntity,
  titleAction?: "replace" | "append"
) => {
  wikidataIdSelection.value = selectedSuggestion.id ?? ""

  if (titleAction) {
    newTitle.value = calculateNewTitle(
      newTitle.value,
      selectedSuggestion,
      titleAction
    )
    hasTitleBeenEdited.value = true
  } else {
    // When titles match (no titleAction), replace with the exact label from Wikidata
    newTitle.value = selectedSuggestion.label
    hasTitleBeenEdited.value = true
  }
}

const onTitleChange = () => {
  hasTitleBeenEdited.value = true
}
</script>

<style lang="sass" scoped>
.title-search-container
  position: relative
  margin-bottom: 1rem

.title-search-results
  margin-top: 0.5rem
  position: relative !important
  height: 200px
  overflow-y: auto
  border: 1px solid hsl(var(--bc) / 0.2)
  border-radius: 4px
  background: hsl(var(--b1))
  box-shadow: 0 2px 4px rgba(0,0,0,0.1)

  :deep(.dropdown-style)
    position: relative !important
    width: 100%
    border: none
    border-radius: 0
    box-shadow: none
    z-index: auto
    background: transparent

  :deep(.dropdown-list)
    max-height: none
    height: auto
    overflow-y: visible
    padding: 0.5rem
    font-size: 0.75rem

    a
      font-size: 0.75rem

      &:hover
        background-color: hsl(var(--b2))

    em
      font-size: 0.75rem
      opacity: 0.7

.secondary-info
  margin-top: 1rem
  padding: 0.5rem
  border: 1px solid #e5e7eb
  border-radius: 0.5rem

  legend
    font-size: 1.125rem
    margin-bottom: 0.5rem
    float: none
    width: auto
</style>
