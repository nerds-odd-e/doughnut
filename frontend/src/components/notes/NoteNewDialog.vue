<template>
  <Breadcrumb v-bind="{ owns: true, notebook, ancestors }">
    <li class="breadcrumb-item">(adding here)</li>
  </Breadcrumb>
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

<script>
import Breadcrumb from "./Breadcrumb.vue";
import NoteFormTitleOnly from "./NoteFormTitleOnly.vue";
import LinkTypeSelect from "../links/LinkTypeSelect.vue";
import storedComponent from "../../store/storedComponent";

function initialState() {
  return {
    creationData: {
      linkTypeToParent: "",
      textContent: {},
    },
    formErrors: {},
  };
}

export default storedComponent({
  components: {
    Breadcrumb,
    NoteFormTitleOnly,
    LinkTypeSelect,
  },
  props: { parentId: [String, Number] },
  data() {
    return {
      loading: true,
      ancestors: null,
      notebook: null,
      ...initialState(),
    };
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.storedApi().getNoteAndItsChildren(this.parentId)
      .then((res) => {
          const note = res.notes[0]
          const { ancestors, notebook } = res.notePosition;
          this.ancestors = [...ancestors, note];
          this.notebook = notebook;
        }
      )
    },

    processForm() {
      this.storedApi().createNote(this.parentId,
        this.creationData
      ).then((res) => {
        this.$router.push({
          name: "noteShow",
          params: { noteId: res.notePosition.noteId },
        })
      })
      .catch((res) => (this.formErrors = res))
    },
  },
});
</script>
