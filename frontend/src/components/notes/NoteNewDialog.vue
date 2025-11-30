<template>
  <div class="daisy-card daisy-w-full">
    <div class="daisy-card-body">
      <form @submit.prevent="processForm">
        <fieldset :disabled="processing">
          <div class="title-search-container">
            <NoteFormTitleOnly
              v-model="creationData.newTitle"
              :error-message="noteFormErrors.newTitle"
              @update:model-value="onTitleChange"
            >
              <template #input_append>
                <WikidataSearchByLabel
                  :search-key="creationData.newTitle"
                  v-model="creationData.wikidataId"
                  :error-message="noteFormErrors.wikidataId"
                  @selected="onSelectWikidataEntry"
                />
              </template>
            </NoteFormTitleOnly>
            <SearchResults
              v-bind="{
                noteId: referenceNote.id,
                inputSearchKey: effectiveSearchKey,
                isDropdown: true,
                notebookId: notebookId
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
} from "@generated/backend"
import type { InsertMode } from "@/models/InsertMode"
import { ref, computed } from "vue"
import SearchResults from "../search/SearchResults.vue"
import NoteFormTitleOnly from "./NoteFormTitleOnly.vue"
import WikidataSearchByLabel from "./WikidataSearchByLabel.vue"
import { useRouter } from "vue-router"
import { calculateNewTitle } from "@/utils/wikidataTitleActions"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const router = useRouter()
const storageAccessor = useStorageAccessor()

// Props
const props = defineProps<{
  referenceNote: Note
  insertMode: InsertMode
}>()

const noteRealm = computed(
  () => storageAccessor.value.refOfNoteRealm(props.referenceNote.id).value
)
const notebookId = computed(() => noteRealm.value?.notebook?.id)

// Emits
const emit = defineEmits<{
  closeDialog: []
}>()

// Reactive state
const creationData = ref<NoteCreationDto>({
  newTitle: "Untitled",
  wikidataId: "",
})

const noteFormErrors = ref({
  newTitle: undefined as undefined | string,
  wikidataId: undefined as undefined | string,
})

const processing = ref(false)
const hasTitleBeenEdited = ref(false)

// Computed property to determine effective search key
const effectiveSearchKey = computed(() => {
  // Don't search if title hasn't been edited yet
  if (!hasTitleBeenEdited.value) {
    return ""
  }
  return creationData.value.newTitle
})

// Methods
const processForm = async () => {
  if (processing.value) return
  processing.value = true
  noteFormErrors.value.wikidataId = undefined
  noteFormErrors.value.newTitle = undefined

  const api = storageAccessor.value.storedApi()
  try {
    if (props.insertMode === "as-child") {
      await api.createNote(router, props.referenceNote.id, creationData.value)
    } else {
      await api.createNoteAfter(
        router,
        props.referenceNote.id,
        creationData.value
      )
    }
    emit("closeDialog")
  } catch (res: unknown) {
    noteFormErrors.value = {
      newTitle: undefined,
      wikidataId: undefined,
      ...(res as object),
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
    height: 100%
    overflow-y: auto
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
