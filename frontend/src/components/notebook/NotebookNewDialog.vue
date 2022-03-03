<template>
  <Breadcrumb
    v-bind="{
      owns: true,
      ancestors: [],
      notebook: { ownership: { circle } },
    }"
  >
    <li class="breadcrumb-item">(adding here)</li>
  </Breadcrumb>

  <LoadingPage v-bind="{ loading, contentExists: true }">
    <form @submit.prevent.once="processForm">
      <NoteFormTitleOnly v-model="noteFormData" :errors="formErrors" />
      <input type="submit" value="Submit" class="btn btn-primary" />
    </form>
  </LoadingPage>
</template>

<script>
import Breadcrumb from "../notes/Breadcrumb.vue";
import NoteFormTitleOnly from "../notes/NoteFormTitleOnly.vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import storedApi from  "../../managedApi/storedApi";

export default {
  props: { circle: Object },
  components: {
    Breadcrumb,
    NoteFormTitleOnly,
    LoadingPage,
  },
  data() {
    return {
      loading: false,
      noteFormData: {},
      formErrors: {},
    };
  },
  methods: {

    processForm() {
      storedApi(this).createNotebook(this.circle,
        this.noteFormData,
      )
        .then((res) =>
          this.$router.push({
            name: "noteShow",
            params: { noteId: res.noteId },
          })
        )
        .catch((res) => (this.formErrors = res))
    },
  },
};
</script>
