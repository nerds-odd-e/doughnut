<template>
  <div class="daisy-card w-full">
    <div class="daisy-card-body">
      <form data-testid="note-new-form" @submit.prevent="processForm">
        <fieldset :disabled="processing">
          <div class="mb-4">
            <p class="text-sm mb-2">Folder</p>
            <p class="text-xs opacity-70 mb-2">{{ parentLocationDescription }}</p>
            <FolderSelector
              v-model="selectedFolder"
              :notebook-id="notebookId"
              :context-folder="folderSelectorContextFolder"
              :ancestor-folders="ancestorFolders"
              :disabled="processing"
            />
          </div>
          <NoteCreationParentRelationship
            v-if="contextNote"
            v-model="parentRelationship"
            :context-content="contextNote.content"
          />
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
              v-model:semantic-search-enabled="semanticSearchEnabled"
              :note-id="titleSearchScopeNote?.id"
              :input-search-key="effectiveSearchKey"
              :is-dropdown="true"
              :notebook-id="notebookId"
              embed-semantic-toggle
              class="title-search-results"
            />
          </div>
          <input
            type="submit"
            value="Submit"
            class="daisy-btn daisy-btn-primary mt-4"
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
import NoteCreationParentRelationship from "./NoteCreationParentRelationship.vue"
import PathNameEditor from "./core/PathNameEditor.vue"
import WikidataSearchByLabel from "./WikidataSearchByLabel.vue"
import { useRouter } from "vue-router"
import {
  calculateNewTitle,
  appendAliasToNoteContent,
} from "@/utils/wikidataTitleActions"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import usePopups from "@/components/commons/Popups/usePopups"
import { createNoteFromForm } from "./noteNewFormSubmit"
import {
  applyParentRelationshipToCreateContent,
  type NoteCreationParentRelationship as ParentRelationship,
} from "@/utils/noteCreationParentRelationship"

function contentWithWikidataFrontmatter(
  wikidataId: string
): string | undefined {
  const t = wikidataId.trim()
  return t ? `---\nwikidata_id: ${t}\n---\n` : undefined
}

const router = useRouter()
const storageAccessor = useStorageAccessor()
const { popups } = usePopups()

const props = withDefaults(
  defineProps<{
    notebookId: number
    initialFolder?: Folder
    initialTitle?: string
    /** When set, title search is scoped under this note. */
    titleSearchAnchorNote?: Note
    wikiTitleCacheRefreshSourceNoteId?: number
    ancestorFolders?: Folder[]
  }>(),
  { ancestorFolders: () => [] }
)

const titleSearchScopeNote = computed(() => props.titleSearchAnchorNote)

const contextNote = computed(() => {
  const note = props.titleSearchAnchorNote
  if (note == null) return undefined
  return { title: note.noteTopology.title, content: note.content }
})

const parentRelationship = ref<ParentRelationship>("none")
const selectedFolder = ref<Folder | null>(props.initialFolder ?? null)

watch(
  () => props.initialFolder,
  (f) => {
    selectedFolder.value = f ?? null
  }
)

const parentLocationDescription = computed(() => {
  const folder = selectedFolder.value ?? props.initialFolder ?? null
  if (folder == null) return "Adds to the notebook root."
  const found = props.ancestorFolders.find((f) => f.id === folder.id)
  const name = found?.name ?? folder.name
  return name != null
    ? `Adds to folder "${name}".`
    : "Adds to the notebook root."
})

const folderSelectorContextFolder = computed(
  (): Folder | null => selectedFolder.value ?? props.initialFolder ?? null
)

const emit = defineEmits<{
  closeDialog: []
}>()

const newTitle = ref(props.initialTitle ?? "Untitled")
const wikidataIdSelection = ref("")
const noteContentMarkdown = ref<string | undefined>(undefined)
const noteFormErrors = ref<{
  newTitle?: string
  wikidataId?: string
}>({
  newTitle: undefined,
  wikidataId: undefined,
})
const processing = ref(false)
const hasTitleBeenEdited = ref(props.initialTitle !== undefined)
const semanticSearchEnabled = ref(false)

const effectiveSearchKey = computed(() =>
  hasTitleBeenEdited.value ? newTitle.value : ""
)

function contentForSubmit(): string | undefined {
  const baseContent =
    noteContentMarkdown.value !== undefined
      ? noteContentMarkdown.value
      : contentWithWikidataFrontmatter(wikidataIdSelection.value)
  return applyParentRelationshipToCreateContent(
    baseContent,
    parentRelationship.value,
    contextNote.value
  )
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

  const content = contentForSubmit()
  const body: NoteCreationDto = {
    newTitle: newTitle.value,
    ...(content !== undefined ? { content } : {}),
  }
  try {
    await createNoteFromForm({
      api: storageAccessor.value.storedApi(),
      router,
      popups,
      notebookId: props.notebookId,
      body,
      folderId: selectedFolder.value?.id ?? undefined,
      refreshWikiTitleCacheForNoteIds:
        props.wikiTitleCacheRefreshSourceNoteId != null
          ? [props.wikiTitleCacheRefreshSourceNoteId]
          : undefined,
      onFieldErrors: (errors) => {
        noteFormErrors.value = errors
      },
      onSuccess: () => emit("closeDialog"),
    })
  } finally {
    processing.value = false
  }
}

const onSelectWikidataEntry = (
  selectedSuggestion: WikidataSearchEntity,
  titleAction?: "replace" | "append"
) => {
  wikidataIdSelection.value = selectedSuggestion.id ?? ""

  if (titleAction === "append") {
    const baseContent =
      noteContentMarkdown.value ??
      contentWithWikidataFrontmatter(wikidataIdSelection.value) ??
      ""
    const appended = appendAliasToNoteContent(
      baseContent,
      selectedSuggestion.label
    )
    if (appended !== null) {
      noteContentMarkdown.value = appended
    }
    hasTitleBeenEdited.value = true
    return
  }

  if (titleAction) {
    newTitle.value = calculateNewTitle(
      newTitle.value,
      selectedSuggestion,
      titleAction
    )
  } else {
    newTitle.value = selectedSuggestion.label
  }
  hasTitleBeenEdited.value = true
}

const onTitleChange = () => {
  hasTitleBeenEdited.value = true
}
</script>

<style lang="sass" scoped src="./NoteNewForm.sass"></style>
