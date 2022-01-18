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
      <NoteFormTitleOnly v-model="noteFormData" :errors="noteFormErrors" />
      <input type="submit" value="Submit" class="btn btn-primary" />
    </form>
  </LoadingPage>
</template>

<script>
import Breadcrumb from "../notes/Breadcrumb.vue";
import NoteFormTitleOnly from "../notes/NoteFormTitleOnly.vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import { restPostMultiplePartForm } from "../../restful/restful";

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
      noteFormErrors: {},
    };
  },
  methods: {
    url() {
      if (!!this.circle) {
        return `/api/circles/${this.circle.id}/notebooks`;
      }
      return `/api/notebooks/create`;
    },
    processForm() {
      restPostMultiplePartForm(
        this.url(),
        this.noteFormData,
        (r) => (this.loading = r)
      )
        .then((res) =>
          this.$router.push({
            name: "noteShow",
            params: { noteId: res.noteId },
          })
        )
        .catch((res) => (this.noteFormErrors = res));
    },
  },
};
</script>
