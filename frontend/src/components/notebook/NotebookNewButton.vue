<template>
  <ModalWithButton v-model="show">
    <template #button>
      <button
        class="btn btn-sm"
        role="button"
        @click="show = true"
        title="Edit notebook settings"
      >
        <slot />
      </button>
    </template>
    <template #header>
      <NoteOwnerBreadcrumb v-bind="{ ancestors: [], notebook:{ ownership: { circle}} }">
        <li class="breadcrumb-item">(adding here)</li>
      </NoteOwnerBreadcrumb>
    </template>

    <template #body>
      <LoadingPage v-bind="{ loading, contentExists: true }">
        <form @submit.prevent.once="processForm">
          <NoteFormBody v-model="noteFormData" :errors="noteFormErrors" />
          <input type="submit" value="Submit" class="btn btn-primary" />
        </form>
      </LoadingPage>
    </template>
  </ModalWithButton>
</template>

<script>
import ModalWithButton from "../commons/ModalWithButton.vue";
import NoteOwnerBreadcrumb from "../notes/NoteOwnerBreadcrumb.vue";
import NoteFormBody from "../notes/NoteFormBody.vue";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import { restPostMultiplePartForm } from "../../restful/restful";

export default {
  name: "NotebookNewPage",
  props: { circle: Object },
  components: {
    ModalWithButton,
    NoteOwnerBreadcrumb,
    NoteFormBody,
    LoadingPage,
  },
  data() {
    return {
      show: false,
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
