<template>
  <form @submit.prevent="processForm">
    <fieldset :disabled="processing">
      <LinkTypeSelectCompact
        scope-name="note"
        field="linkTypeToParent"
        :allow-empty="true"
        v-model="creationData.linkTypeToParent"
        :error-message="noteFormErrors.linkTypeToParent"
      />
      <NoteFormTopicOnly
        v-model="creationData.topicConstructor"
        :error-message="noteFormErrors.topicConstructor"
      />
      <SuggestTopic
        :original-topic="creationData.topicConstructor"
        :suggested-topic="suggestedTopic"
        @suggested-topic-selected="takeSuggestedTopic"
      />
      <WikidataSearchByLabel
        :search-key="creationData.topicConstructor"
        v-model="creationData.wikidataId"
        :error-message="noteFormErrors.wikidataId"
        @selected="onSelectWikidataEntry"
      />
      <input type="submit" value="Submit" class="btn btn-primary" />
      <fieldset class="secondary-info">
        <legend>Similar Notes</legend>
        <SearchResults
          v-bind="{
            noteId: parentId,
            inputSearchKey: creationData.topicConstructor,
          }"
        />
      </fieldset>
    </fieldset>
  </form>
</template>

<script lang="ts">
import type { WikidataSearchEntity } from "@/generated/backend"
import { NoteCreationDTO } from "@/generated/backend"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import LinkTypeSelectCompact from "../links/LinkTypeSelectCompact.vue"
import SearchResults from "../search/SearchResults.vue"
import NoteFormTopicOnly from "./NoteFormTopicOnly.vue"
import SuggestTopic from "./SuggestTopic.vue"
import WikidataSearchByLabel from "./WikidataSearchByLabel.vue"

export default defineComponent({
  components: {
    NoteFormTopicOnly,
    SearchResults,
    LinkTypeSelectCompact,
    WikidataSearchByLabel,
    SuggestTopic,
  },
  props: {
    parentId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["closeDialog"],
  data() {
    return {
      creationData: <NoteCreationDTO>{
        linkTypeToParent: "no link",
        topicConstructor: "",
        wikidataId: "",
      },
      noteFormErrors: {
        linkTypeToParent: undefined,
        topicConstructor: undefined as undefined | string,
        wikidataId: undefined as undefined | string,
      },
      suggestedTopic: "",
      processing: false,
    }
  },
  methods: {
    processForm() {
      if (this.processing) return
      this.processing = true
      this.noteFormErrors.wikidataId = undefined
      this.noteFormErrors.topicConstructor = undefined
      this.storageAccessor
        .storedApi()
        .createNote(this.$router, this.parentId, this.creationData)
        .then(() => {
          this.$emit("closeDialog")
        })
        .catch((res) => {
          this.noteFormErrors = res
        })
        .finally(() => {
          this.processing = false
        })
    },
    onSelectWikidataEntry(selectedSuggestion: WikidataSearchEntity) {
      const currentLabel = this.creationData.topicConstructor.toUpperCase()
      const newLabel = selectedSuggestion.label.toUpperCase()

      if (currentLabel === newLabel) {
        this.creationData.topicConstructor = selectedSuggestion.label
        this.suggestedTopic = ""
      } else {
        this.suggestedTopic = selectedSuggestion.label
      }

      this.creationData.wikidataId = selectedSuggestion.id
    },
    takeSuggestedTopic(topic: string) {
      this.creationData.topicConstructor = topic
      this.suggestedTopic = ""
    },
  },
})
</script>

<style lang="sass">
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
