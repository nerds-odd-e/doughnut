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
              ref="pathNameEditor"
              v-model="newTitle"
              :error-message="noteFormErrors.newTitle"
              autofocus
              :initial-select-all="initialTitle === undefined"
              warn-on-note-title-compatibility
              @update:model-value="onTitleChange"
            >
              <template #append>
                <SpeakTitleControl
                  joins-field
                  v-model:busy="titleSpeechBusy"
                  @heard-segments="onHeardTitleSegments"
                />
                <WikidataSearchByLabel
                  :search-key="newTitle"
                  v-model="wikidataIdSelection"
                  :error-message="noteFormErrors.wikidataId"
                  @selected="onSelectWikidataEntry"
                />
              </template>
            </PathNameEditor>
            <SearchResults
              :note-id="titleSearchScopeNote?.id"
              :input-search-key="effectiveSearchKey"
              :is-dropdown="true"
              :notebook-id="notebookId"
              :list-mode-toggle="false"
              class="title-search-results"
            />
          </div>
          <input
            type="submit"
            value="Submit"
            class="daisy-btn daisy-btn-primary mt-4"
            :disabled="titleSpeechBusy"
          />
        </fieldset>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  FolderTrailSegment,
  WikidataSearchEntity,
  Note,
  NoteCreationDto,
} from "@generated/donut-backend-api"
import { ref, computed, watch } from "vue"
import SearchResults from "../search/SearchResults.vue"
import FolderSelector from "./FolderSelector.vue"
import NoteCreationParentRelationship from "./NoteCreationParentRelationship.vue"
import PathNameEditor from "./core/PathNameEditor.vue"
import SpeakTitleControl from "./SpeakTitleControl.vue"
import WikidataSearchByLabel from "./WikidataSearchByLabel.vue"
import { useRouter } from "vue-router"
import { useNoteStore } from "@/store/noteStore"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  dictatedInsertion,
  joinDictatedSegments,
} from "@/models/audio/joinDictatedSegments"
import { contentForNewNote, createNoteFromForm } from "./noteNewFormSubmit"
import { heardWordsReplaceTitle, initialNewNoteTitle } from "./noteNewFormTitle"
import { applyWikidataEntryToNewNoteForm } from "./noteNewFormWikidata"
import type { NoteCreationParentRelationship as ParentRelationship } from "@/utils/noteCreationParentRelationship"

const router = useRouter()
const noteStore = useNoteStore()
const { popups } = usePopups()

const props = withDefaults(
  defineProps<{
    notebookId: number
    initialFolder?: FolderTrailSegment
    initialTitle?: string
    /** When set, title search is scoped under this note. */
    titleSearchAnchorNote?: Note
    ancestorFolders?: FolderTrailSegment[]
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
const selectedFolder = ref<FolderTrailSegment | null>(
  props.initialFolder ?? null
)

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
  (): FolderTrailSegment | null =>
    selectedFolder.value ?? props.initialFolder ?? null
)

const emit = defineEmits<{
  closeDialog: []
}>()

const newTitle = ref(initialNewNoteTitle(props.initialTitle))
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
const titleSpeechBusy = ref(false)
const hasTitleBeenEdited = ref(props.initialTitle !== undefined)
const pathNameEditor = ref<InstanceType<typeof PathNameEditor> | null>(null)
const effectiveSearchKey = computed(() =>
  hasTitleBeenEdited.value ? newTitle.value : ""
)

const processForm = async () => {
  if (processing.value || titleSpeechBusy.value) return
  processing.value = true
  noteFormErrors.value.wikidataId = undefined
  noteFormErrors.value.newTitle = undefined

  const trimmedWikidata = wikidataIdSelection.value.trim()
  if (trimmedWikidata !== "" && !/^Q\d+$/i.test(trimmedWikidata)) {
    noteFormErrors.value.wikidataId = "The wikidata Id should be Q<numbers>"
    processing.value = false
    return
  }

  const content = contentForNewNote({
    noteContentMarkdown: noteContentMarkdown.value,
    wikidataId: wikidataIdSelection.value,
    parentRelationship: parentRelationship.value,
    contextNote: contextNote.value,
  })
  const body: NoteCreationDto = {
    newTitle: newTitle.value,
    ...(content !== undefined ? { content } : {}),
  }
  try {
    await createNoteFromForm({
      api: noteStore,
      router,
      popups,
      notebookId: props.notebookId,
      body,
      folderId: selectedFolder.value?.id ?? undefined,
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
) =>
  applyWikidataEntryToNewNoteForm(selectedSuggestion, titleAction, {
    wikidataIdSelection,
    newTitle,
    noteContentMarkdown,
    hasTitleBeenEdited,
  })

const onTitleChange = () => {
  hasTitleBeenEdited.value = true
}

const onHeardTitleSegments = (segments: string[]) => {
  if (heardWordsReplaceTitle(hasTitleBeenEdited.value, newTitle.value)) {
    pathNameEditor.value?.replaceText(joinDictatedSegments("", segments))
    return
  }
  pathNameEditor.value?.insertAtSelection((before, after) =>
    dictatedInsertion(before, segments, after)
  )
}
</script>

<style lang="sass" scoped src="./NoteNewForm.sass"></style>
