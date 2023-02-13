<template>
  <form @submit.prevent.once="processForm">
    <NoteFormTitleOnly v-model="noteFormData" :errors="errors" />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script>
import NoteFormTitleOnly from "../notes/NoteFormTitleOnly.vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  props: { circle: Object },
  components: {
    NoteFormTitleOnly,
  },
  data() {
    return {
      noteFormData: {},
      errors: {},
    };
  },
  methods: {
    processForm() {
      this.api.notebookMethods
        .createNotebook(this.circle, this.noteFormData)
        .then((res) =>
          this.$router.push({
            name: "noteShow",
            params: { noteId: res.noteId },
          })
        )
        .catch((err) => (this.errors = err));
    },
  },
};
</script>
