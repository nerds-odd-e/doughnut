<template>
  <form @submit.prevent="processForm">
    <LinkTypeSelect
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
    <input type="submit" value="Submit" class="btn btn-primary" />
    <SearchResults
      v-bind="{
        noteId: parentId,
        inputSearchKey: creationData.textContent.title,
      }"
    />
  </form>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteFormTitleOnly from "./NoteFormTitleOnly.vue";
import LinkTypeSelect from "../links/LinkTypeSelect.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import SearchResults from "../search/SearchResults.vue";

function initialState() {
  return {
    creationData: {
      linkTypeToParent: "",
      textContent: { title: "" },
    },
  };
}

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true, hasFormError: true });
  },
  components: {
    NoteFormTitleOnly,
    LinkTypeSelect,
    SearchResults,
  },
  props: { parentId: Number },
  data() {
    return {
      ...initialState(),
    };
  },
  mounted() {
    this.fetchData();
  },
  methods: {
    fetchData() {
      this.storedApi.getNoteRealmWithPosition(this.parentId);
    },

    processForm() {
      this.storedApi
        .createNote(this.parentId, this.creationData)
        .then((res) => {
          this.$router.push({
            name: "noteShow",
            params: { rawNoteId: res.notePosition.noteId },
          });
        })
        .catch((res) => (this.formErrors = res));
    },
  },
});
</script>
