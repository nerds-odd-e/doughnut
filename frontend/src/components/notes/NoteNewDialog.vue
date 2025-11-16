<template>
  <div class="daisy-card daisy-w-full">
    <div class="daisy-card-body">
      <form @submit.prevent="processForm">
        <fieldset :disabled="processing">
          <div class="title-search-container">
            <NoteFormTitleOnly
              v-model="creationData.newTitle"
              :error-message="noteFormErrors.newTitle"
              @focus="showDropdown = true"
              @blur="onTitleBlur"
            />
            <SuggestTitle
              :original-title="creationData.newTitle"
              :suggested-title="suggestedTitle"
              @suggested-title-selected="takeSuggestedTitle"
            />
            <SearchResults
              v-show="shouldShowSearch"
              v-bind="{
                noteId: referenceNote.id,
                inputSearchKey: searchKey,
                isDropdown: true
              }"
              class="title-search-results"
            />
          </div>

          <WikidataSearchByLabel
            :search-key="searchKey"
            v-model="creationData.wikidataId"
            :error-message="noteFormErrors.wikidataId"
            @selected="onSelectWikidataEntry"
          />
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
  NoteCreationDTO,
} from "@generated/backend"
import type { InsertMode } from "@/models/InsertMode"
import type { StorageAccessor } from "../../store/createNoteStorage"
import { ref, computed } from "vue"
import SearchResults from "../search/SearchResults.vue"
import NoteFormTitleOnly from "./NoteFormTitleOnly.vue"
import SuggestTitle from "./SuggestTitle.vue"
import WikidataSearchByLabel from "./WikidataSearchByLabel.vue"
import { useRouter } from "vue-router"

const router = useRouter()

// Props
const props = defineProps<{
  referenceNote: Note
  insertMode: InsertMode
  storageAccessor: StorageAccessor
}>()

// Emits
const emit = defineEmits<{
  closeDialog: []
}>()

// Reactive state
const creationData = ref<NoteCreationDTO>({
  newTitle: "Untitled",
  wikidataId: "",
})

const noteFormErrors = ref({
  newTitle: undefined as undefined | string,
  wikidataId: undefined as undefined | string,
})

const suggestedTitle = ref("")
const processing = ref(false)
const showDropdown = ref(false)

const DEFAULT_TITLE = "Untitled"
const shouldShowSearch = computed(() => {
  return (
    showDropdown.value &&
    creationData.value.newTitle &&
    creationData.value.newTitle !== DEFAULT_TITLE
  )
})

const searchKey = computed(() => {
  return creationData.value.newTitle === DEFAULT_TITLE
    ? ""
    : creationData.value.newTitle
})

// Methods
const processForm = async () => {
  if (processing.value) return
  processing.value = true
  noteFormErrors.value.wikidataId = undefined
  noteFormErrors.value.newTitle = undefined

  const api = props.storageAccessor.storedApi()
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

const onSelectWikidataEntry = (selectedSuggestion: WikidataSearchEntity) => {
  const currentLabel = creationData.value.newTitle.toUpperCase()
  const newLabel = selectedSuggestion.label.toUpperCase()

  if (currentLabel === newLabel) {
    creationData.value.newTitle = selectedSuggestion.label
    suggestedTitle.value = ""
  } else {
    suggestedTitle.value = selectedSuggestion.label
  }

  creationData.value.wikidataId = selectedSuggestion.id
}

const takeSuggestedTitle = (title: string) => {
  creationData.value.newTitle = title
  suggestedTitle.value = ""
}

const onTitleBlur = () => {
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}
</script>

<style lang="sass" scoped>
.title-search-container
  position: relative
  margin-bottom: 1rem

.title-search-results
  position: absolute
  top: 100%
  left: 0
  right: 0
  z-index: 1000

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
