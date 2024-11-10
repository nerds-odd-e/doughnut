<template>
  <form @submit.prevent="processForm">
    <fieldset :disabled="processing">
      <div class="topic-search-container">
        <NoteFormTopicOnly
          v-model="creationData.topicConstructor"
          :error-message="noteFormErrors.topicConstructor"
          @focus="showDropdown = true"
          @blur="onTopicBlur"
        />
        <SuggestTopic
          :original-topic="creationData.topicConstructor"
          :suggested-topic="suggestedTopic"
          @suggested-topic-selected="takeSuggestedTopic"
        />
        <SearchResults
          v-show="showDropdown && creationData.topicConstructor"
          v-bind="{
            noteId: referenceNote.id,
            inputSearchKey: creationData.topicConstructor,
            isDropdown: true
          }"
          class="topic-search-results"
        />
      </div>

      <WikidataSearchByLabel
        :search-key="creationData.topicConstructor"
        v-model="creationData.wikidataId"
        :error-message="noteFormErrors.wikidataId"
        @selected="onSelectWikidataEntry"
      />
      <input type="submit" value="Submit" class="btn btn-primary" />
    </fieldset>
  </form>
</template>

<script setup lang="ts">
import type {
  WikidataSearchEntity,
  Note,
  NoteCreationDTO,
} from "@/generated/backend"
import type { InsertMode } from "@/models/InsertMode"
import type { StorageAccessor } from "../../store/createNoteStorage"
import { ref } from "vue"
import SearchResults from "../search/SearchResults.vue"
import NoteFormTopicOnly from "./NoteFormTopicOnly.vue"
import SuggestTopic from "./SuggestTopic.vue"
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
  topicConstructor: "Untitled",
  wikidataId: "",
})

const noteFormErrors = ref({
  topicConstructor: undefined as undefined | string,
  wikidataId: undefined as undefined | string,
})

const suggestedTopic = ref("")
const processing = ref(false)
const showDropdown = ref(false)

// Methods
const processForm = async () => {
  if (processing.value) return
  processing.value = true
  noteFormErrors.value.wikidataId = undefined
  noteFormErrors.value.topicConstructor = undefined

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
      topicConstructor: undefined,
      wikidataId: undefined,
      ...(res as object),
    }
  } finally {
    processing.value = false
  }
}

const onSelectWikidataEntry = (selectedSuggestion: WikidataSearchEntity) => {
  const currentLabel = creationData.value.topicConstructor.toUpperCase()
  const newLabel = selectedSuggestion.label.toUpperCase()

  if (currentLabel === newLabel) {
    creationData.value.topicConstructor = selectedSuggestion.label
    suggestedTopic.value = ""
  } else {
    suggestedTopic.value = selectedSuggestion.label
  }

  creationData.value.wikidataId = selectedSuggestion.id
}

const takeSuggestedTopic = (topic: string) => {
  creationData.value.topicConstructor = topic
  suggestedTopic.value = ""
}

const onTopicBlur = () => {
  setTimeout(() => {
    showDropdown.value = false
  }, 200)
}
</script>

<style lang="sass" scoped>
.topic-search-container
  position: relative
  margin-bottom: 1rem

.topic-search-results
  position: absolute
  top: 100%
  left: 0
  right: 0
  z-index: 1000

.secondary-info
  margin-top: 1rem
  padding: 5px
  margin: 0
  border: 1px solid #ccc
  border-radius: 4px
  color: #999
  font-size: smaller

  legend
    font-size: 1.2rem
    margin-bottom: 0.5rem
    float: none
    width: auto
</style>
