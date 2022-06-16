<template>
  <form v-show="!selectedOption" @submit.prevent="processForm">
    <LinkTypeSelectCompact
      scope-name="note"
      field="linkTypeToParent"
      :allow-empty="true"
      v-model="creationData.linkTypeToParent"
      :errors="formErrors.linkTypeToParent"
    />
    <NoteFormTitleOnly
      v-model="creationData.textContent"
      :errors="formErrors.textContent"
    />
    <TextInput
      scope-name="wikidataID"
      field="wikidataID"
      v-model="creationData.wikidataId"
      :errors="formErrors.wikidataId"
      placeholder="example: `Q1234`"
    />
    <div class="row mt-2 mb-2">
      <div class="col-6 btn-group" role="group" aria-label="Action Group">
        <input type="submit" value="Submit" class="btn btn-primary" />
        <button
          class="btn btn-outline-primary"
          @click.prevent="fetchSearchResult"
        >
          Search on Wikidata
        </button>
      </div>
      <div class="col-6">
        <select
          name="wikidataSearchResult"
          @change="onSelectSearchResult"
          class="form-control"
          v-model="selectedOption"
        >
          <option disabled value="">- Choose Wikidata Search Result -</option>
          <option
            v-for="suggestion in wikiSearchSuggestions"
            :key="suggestion.id"
            :value="suggestion.id"
            scope-name="searchItem"
          >
            {{ suggestion.label }} - {{ suggestion.description }}
          </option>
        </select>
      </div>
    </div>
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
  <form v-show="selectedOption" @submit.prevent="acceptSuggestion">
    Are you sure want to replace the title with the title from Wikidata?
    <br />
    <strong
      >{{ creationData.textContent.title }} >
      {{ selectedWikidataEntry.label }}</strong
    >
    <br />
    <input
      type="cancel"
      value="Cancel"
      class="btn btn-secondary"
      @click="selectedOption = ''"
    />
    <input
      name="acceptSuggestion"
      type="submit"
      value="Yes"
      class="btn btn-primary"
      @click="acceptSuggestion"
    />
  </form>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import TextInput from "../form/TextInput.vue";
import NoteFormTitleOnly from "./NoteFormTitleOnly.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import SearchResults from "../search/SearchResults.vue";
import LinkTypeSelectCompact from "../links/LinkTypeSelectCompact.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true, hasFormError: true });
  },
  components: {
    NoteFormTitleOnly,
    SearchResults,
    LinkTypeSelectCompact,
    TextInput,
  },
  props: { parentId: { type: Number, required: true } },
  emits: ["done"],
  data() {
    return {
      creationData: {
        linkTypeToParent: "no link",
        textContent: { title: "" },
        wikidataId: "",
      } as Generated.NoteCreation,
      formErrors: {
        linkTypeToParent: undefined,
        textContent: {},
        wikidataId: undefined,
      },
      wikiSearchSuggestions: [] as Generated.WikidataSearchEntity[],
      selectedOption: "",
      selectedWikidataEntry: {} as Generated.WikidataSearchEntity,
    };
  },
  methods: {
    processForm() {
      this.storedApi
        .createNote(this.parentId, this.creationData)
        .then((res) => {
          this.$emit("done", res);
        })
        .catch((res) => (this.formErrors = res));
    },
    async onSelectSearchResult() {
      const selectedSuggestion = this.wikiSearchSuggestions.find((obj) => {
        return obj.id === this.selectedOption;
      });
      if (selectedSuggestion) {
        this.selectedWikidataEntry = selectedSuggestion;
      }
    },
    async acceptSuggestion() {
      this.creationData.textContent.title = this.selectedWikidataEntry.label;
      this.creationData.wikidataId = this.selectedWikidataEntry.id;
      this.selectedOption = "";
    },
    async fetchSearchResult() {
      if (this.creationData.textContent.title) {
        this.wikiSearchSuggestions = await this.api.wikidata.getWikiDatas(
          this.creationData.textContent.title
        );
        if (this.wikiSearchSuggestions.length === 0) {
          this.selectedOption = "";
        }
      } else {
        this.wikiSearchSuggestions = [];
        this.selectedOption = "";
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
