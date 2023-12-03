<template>
  <form @submit.prevent="processForm">
    <fieldset :disabled="processing">
      <LinkTypeSelectCompact
        scope-name="note"
        field="linkTypeToParent"
        :allow-empty="true"
        v-model="creationData.linkTypeToParent"
        :errors="noteFormErrors.linkTypeToParent"
      />
      <NoteFormTopicOnly
        v-model="creationData.textContent"
        :errors="noteFormErrors.textContent"
      />
      <SuggestTopic
        :original-topic="creationData.textContent.topic"
        :suggested-topic="suggestedTopic"
        @suggested-topic-selected="takeSuggestedTopic"
      />
      <WikidataSearchByLabel
        :search-key="creationData.textContent.topic"
        v-model="creationData.wikidataId"
        :errors="noteFormErrors.wikidataId"
        @selected="onSelectWikidataEntry"
      />
      <input type="submit" value="Submit" class="btn btn-primary" />
      <fieldset class="secondary-info">
        <legend>Similar Notes</legend>
        <SearchResults
          v-bind="{
            noteId: parentId,
            inputSearchKey: creationData.textContent.topic,
          }"
        />
      </fieldset>
    </fieldset>
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteFormTopicOnly from "./NoteFormTopicOnly.vue";
import SearchResults from "../search/SearchResults.vue";
import LinkTypeSelectCompact from "../links/LinkTypeSelectCompact.vue";
import WikidataSearchByLabel from "./WikidataSearchByLabel.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import SuggestTopic from "./SuggestTopic.vue";

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
      creationData: {
        linkTypeToParent: "no link",
        textContent: { topic: "" },
        wikidataId: "",
      } as Generated.NoteCreation,
      noteFormErrors: {
        linkTypeToParent: undefined,
        textContent: {},
        wikidataId: undefined as undefined | string,
      },
      suggestedTopic: "",
      processing: false,
    };
  },
  methods: {
    processForm() {
      if (this.processing) return;
      this.processing = true;
      this.noteFormErrors.wikidataId = undefined;
      this.noteFormErrors.textContent = {};
      this.storageAccessor
        .api(this.$router)
        .createNote(this.parentId, this.creationData)
        .then(() => {
          this.$emit("closeDialog");
        })
        .catch((res) => {
          this.noteFormErrors = res;
        })
        .finally(() => {
          this.processing = false;
        });
    },
    onSelectWikidataEntry(selectedSuggestion: Generated.WikidataSearchEntity) {
      const currentLabel = this.creationData.textContent.topic.toUpperCase();
      const newLabel = selectedSuggestion.label.toUpperCase();

      if (currentLabel === newLabel) {
        this.creationData.textContent.topic = selectedSuggestion.label;
        this.suggestedTopic = "";
      } else {
        this.suggestedTopic = selectedSuggestion.label;
      }

      this.creationData.wikidataId = selectedSuggestion.id;
    },
    takeSuggestedTopic(topic: string) {
      this.creationData.textContent.topic = topic;
      this.suggestedTopic = "";
    },
  },
});
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
