<template>
  <form @submit.prevent="processForm">
    <LinkTypeSelectCompact
      scope-name="note"
      field="linkTypeToParent"
      :allow-empty="true"
      v-model="creationData.linkTypeToParent"
      :errors="noteFormErrors.linkTypeToParent"
    />
    <NoteFormTitleOnly
      v-model="creationData.textContent"
      :errors="noteFormErrors.textContent"
    />
    <template v-if="suggestedTitle">
      <label>Suggested Title: {{ suggestedTitle }}</label>
      <RadioButtons
        v-model="replaceOrAppendTitle"
        scope-name="titleRadio"
        :options="[
          { value: 'Replace', label: 'Replace title' },
          { value: 'Append', label: 'Append title' },
        ]"
        @change="updateModelValue()"
      />
    </template>
    <TextInput
      scope-name="wikidataID"
      field="wikidataID"
      v-model="creationData.wikidataId"
      :errors="noteFormErrors.wikiDataId"
      placeholder="example: `Q1234`"
    />
    <WikidataSearchByLabel
      :title="creationData.textContent.title"
      @selected="onSelectWikidataEntry"
    />
    <fieldset class="secondary-info">
      <legend>Similar Notes</legend>
      <SearchResults
        v-bind="{
          noteId: parentId,
          inputSearchKey: creationData.textContent.title,
        }"
      />
    </fieldset>
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextInput from "../form/TextInput.vue";
import RadioButtons from "../form/RadioButtons.vue";
import NoteFormTitleOnly from "./NoteFormTitleOnly.vue";
import SearchResults from "../search/SearchResults.vue";
import LinkTypeSelectCompact from "../links/LinkTypeSelectCompact.vue";
import WikidataSearchByLabel from "./WikidataSearchByLabel.vue";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  components: {
    NoteFormTitleOnly,
    SearchResults,
    LinkTypeSelectCompact,
    TextInput,
    RadioButtons,
    WikidataSearchByLabel,
  },
  props: {
    parentId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["done"],
  data() {
    return {
      creationData: {
        linkTypeToParent: "no link",
        textContent: { title: "" },
        wikidataId: "",
      } as Generated.NoteCreation,
      noteFormErrors: {
        linkTypeToParent: undefined,
        textContent: {},
        wikiDataId: undefined as undefined | string,
      },
      replaceOrAppendTitle: "",
      originalTitle: "",
      suggestedTitle: "",
    };
  },
  methods: {
    processForm() {
      this.noteFormErrors.wikiDataId = undefined;
      this.noteFormErrors.textContent = {};
      this.storageAccessor
        .api()
        .createNote(this.parentId, this.creationData)
        .then((res) => {
          this.$emit("done", res);
        })
        .catch((res) => {
          this.noteFormErrors = res;
        });
    },
    onSelectWikidataEntry(selectedSuggestion: Generated.WikidataSearchEntity) {
      const currentLabel = this.creationData.textContent.title.toUpperCase();
      const newLabel = selectedSuggestion.label.toUpperCase();

      if (currentLabel === newLabel) {
        this.creationData.textContent.title = selectedSuggestion.label;
        this.suggestedTitle = "";
      } else {
        this.suggestedTitle = selectedSuggestion.label;
      }

      this.originalTitle = this.creationData.textContent.title;
      this.creationData.wikidataId = selectedSuggestion.id;
    },
    updateModelValue() {
      if (this.replaceOrAppendTitle === "Replace") {
        this.creationData.textContent.title = this.suggestedTitle || "";
      }

      if (this.replaceOrAppendTitle === "Append") {
        this.creationData.textContent.title = `${this.originalTitle} / ${this.suggestedTitle}`;
      }
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
