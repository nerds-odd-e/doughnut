<template>
  <form @submit.prevent="processForm">
    <LinkTypeSelect
      scopeName="note"
      field="linkTypeToParent"
      :allowEmpty="true"
      v-model="creationData.linkTypeToParent"
      :errors="formErrors.linkTypeToParent"
    />
    <NoteFormTitleOnly
      v-model="creationData.textContent"
      :errors="formErrors.textContent"
    />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteFormTitleOnly from "./NoteFormTitleOnly.vue";
import LinkTypeSelect from "../links/LinkTypeSelect.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";

function initialState() {
  return {
    creationData: {
      linkTypeToParent: "",
      textContent: {},
    },
  };
}

export default defineComponent({
  setup() {
    return useStoredLoadingApi({initalLoading: true, hasFormError: true});
  },
  components: {
    NoteFormTitleOnly,
    LinkTypeSelect,
  },
  props: { parentId: Number },
  data() {
    return {
      ...initialState(),
    };
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.storedApi.getNoteAndItsChildren(this.parentId)
    },

    processForm() {
      this.storedApi.createNote(this.parentId,
        this.creationData
      ).then((res) => {
        this.$router.push({
          name: "noteShow",
          params: { rawNoteId: res.notes[0].id },
        })
      })
      .catch((res) => (this.formErrors = res))
    },
  },
});
</script>
