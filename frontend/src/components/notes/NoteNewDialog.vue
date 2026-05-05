<template>
  <div class="daisy-card daisy-w-full">
    <div class="daisy-card-body">
      <form data-testid="note-new-dialog-form" @submit.prevent="processForm">
        <fieldset :disabled="processing">
          <p
            v-if="parentLocationDescription"
            class="daisy-text-sm daisy-opacity-80 daisy-mb-3"
            data-testid="note-new-dialog-parent-location"
          >
            {{ parentLocationDescription }}
          </p>
          <div class="title-search-container">
            <NoteTitleEditor
              v-model="creationData.newTitle"
              :error-message="noteFormErrors.newTitle"
              autofocus
              @update:model-value="onTitleChange"
            >
              <template #append>
                <WikidataSearchByLabel
                  :search-key="creationData.newTitle"
                  v-model="creationData.wikidataId"
                  :error-message="noteFormErrors.wikidataId"
                  @selected="onSelectWikidataEntry"
                />
              </template>
            </NoteTitleEditor>
            <SearchResults
              v-if="titleSearchScopeNote"
              v-bind="{
                noteId: titleSearchScopeNote.id,
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
  WikidataSearchEntity,
  Note,
  NoteCreationDto,
} from "@generated/doughnut-backend-api"
import { ref, computed } from "vue"
import SearchResults from "../search/SearchResults.vue"
import NoteTitleEditor from "./core/NoteTitleEditor.vue"
import WikidataSearchByLabel from "./WikidataSearchByLabel.vue"
import { useRouter } from "vue-router"
import { calculateNewTitle } from "@/utils/wikidataTitleActions"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { toOpenApiError } from "@/managedApi/openApiError"
import usePopups from "@/components/commons/Popups/usePopups"

const router = useRouter()
const storageAccessor = useStorageAccessor()
const { popups } = usePopups()

const props = defineProps<{
  notebookRootNotebookId: number
  /** Scope for create-note when using notebook root API (active sidebar folder). */
  targetFolderId?: number
  /** Shown above the form (e.g. "Folder: …" or "Notebook root"). */
  parentLocationDescription?: string
  initialTitle?: string
  /** Duplicate title search is scoped from this note (e.g. current note in sidebar). */
  titleSearchAnchorNote?: Note
  /** After notebook-root create, refresh wiki title cache for this note before navigating away. */
  wikiTitleCacheRefreshSourceNoteId?: number
}>()

const titleSearchScopeNote = computed(() => props.titleSearchAnchorNote)

// Emits
const emit = defineEmits<{
  closeDialog: []
}>()

// Reactive state
const creationData = ref<NoteCreationDto>({
  newTitle: props.initialTitle ?? "Untitled",
  wikidataId: "",
})

const noteFormErrors = ref({
  newTitle: undefined as undefined | string,
  wikidataId: undefined as undefined | string,
})

const processing = ref(false)
const hasTitleBeenEdited = ref(props.initialTitle !== undefined)

// Computed property to determine effective search key
const effectiveSearchKey = computed(() => {
  // Don't search if title hasn't been edited yet
  if (!hasTitleBeenEdited.value) {
    return ""
  }
  return creationData.value.newTitle
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

  const api = storageAccessor.value.storedApi()
  try {
    await api.createRootNoteAtNotebook(
      router,
      props.notebookRootNotebookId,
      creationData.value,
      {
        folderId: props.targetFolderId,
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
  creationData.value.wikidataId = selectedSuggestion.id

  if (titleAction) {
    creationData.value.newTitle = calculateNewTitle(
      creationData.value.newTitle,
      selectedSuggestion,
      titleAction
    )
    hasTitleBeenEdited.value = true
  } else {
    // When titles match (no titleAction), replace with the exact label from Wikidata
    creationData.value.newTitle = selectedSuggestion.label
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
