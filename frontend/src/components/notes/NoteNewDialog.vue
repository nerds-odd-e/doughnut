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
    <p v-if="noteFormErrors.isDuplicate" style="color: red">
      Duplicate Wikidata ID detected.
    </p>
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
        isDuplicate: false,
      },
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
          const errorCode = res.code;
          if (errorCode === "DUPLICATE") {
            this.noteFormErrors.isDuplicate = true;
          }
        });
    },
    onSelectWikidataEntry(selectedSuggestion: Generated.WikidataSearchEntity) {
      this.creationData.textContent.title = selectedSuggestion.label;
      this.creationData.wikidataId = selectedSuggestion.id;
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
