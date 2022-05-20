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

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true, hasFormError: true });
  },
  components: {
    NoteFormTitleOnly,
    LinkTypeSelect,
    SearchResults,
  },
  props: { parentId: { type: Number, required: true } },
  data() {
    return {
      creationData: {
        linkTypeToParent: 0,
        textContent: { title: "" },
      } as Generated.NoteCreation,
      formErrors: {
        linkTypeToParent: undefined,
        textContent: {},
      },
    };
  },
  methods: {
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
