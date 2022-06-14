<template>
  <form @submit.prevent="processForm">
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
    <div class="row mt-2 mb-2">
      <div class="col-6 btn-group" role="group" aria-label="Action Group">
        <input type="submit" value="Submit" class="btn btn-primary" />
        <button class="btn btn-outline-primary" @click="processForm">
          Search on Wikidata
        </button>
      </div>
      <div class="col-6">
        <select class="form-control" v-model="selectedWikiSuggestion">
          <option disabled value="">- Choose Wikidata Search Result -</option>
          <option
            v-for="suggestion in wikiSearchSuggestions"
            :key="suggestion.id"
            :value="suggestion.id"
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
</template>

<script lang="ts">
import { defineComponent } from "vue";
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
  },
  props: { parentId: { type: Number, required: true } },
  emits: ["done"],
  data() {
    return {
      creationData: {
        linkTypeToParent: "no link",
        textContent: { title: "" },
      } as Generated.NoteCreation,
      formErrors: {
        linkTypeToParent: undefined,
        textContent: {},
      },
      wikiSearchSuggestions: [
        {
          id: "Q12345",
          label: "Snake",
          description: "limbless, scaly, elongate reptile",
        },
      ],
      selectedWikiSuggestion: "",
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
