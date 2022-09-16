<template>
  <LoadingPage v-bind="{ contentExists: true }">
    <form @submit.prevent.once="processForm">
      <NoteFormTitleOnly v-model="noteFormData" :errors="formErrors" />
      <input type="submit" value="Submit" class="btn btn-primary" />
    </form>
  </LoadingPage>
</template>

<script>
import NoteFormTitleOnly from "../notes/NoteFormTitleOnly.vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi({ hasFormError: true });
  },
  props: { circle: Object },
  components: {
    NoteFormTitleOnly,
    LoadingPage,
  },
  data() {
    return {
      noteFormData: {},
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
        );
    },
  },
};
</script>
