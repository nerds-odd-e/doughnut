<template>
  <ModalWithButton v-model="show">
    <template #button>
      <button class="btn btn-small" title="edit note" @click="show = true">
        <SvgEdit />
      </button>
    </template>
    <template #header>
      <h3>{{ oldTitle }}</h3>
    </template>
    <template #body>
      <form @submit.prevent.once="processForm">
        <NoteFormBody
          v-if="!!formData"
          v-model="formData"
          :errors="formErrors"
        />
        <input type="submit" value="Submit" class="btn btn-primary" />
      </form>
    </template>
  </ModalWithButton>
</template>

<script>
import NoteFormBody from "./NoteFormBody.vue";
import ModalWithButton from "../commons/ModalWithButton.vue";
import SvgEdit from "../svgs/SvgEdit.vue";
import { restGet, restPostMultiplePartForm } from "../../restful/restful";

export default {
  name: "NoteNewButton",
  components: {
    NoteFormBody,
    ModalWithButton,
    SvgEdit,
  },
  props: { noteId: Number, oldTitle: String },
  emits: ["updated"],
  data() {
    return {
      show: false,
      formData: null,
      formErrors: {},
      loading: false,
    };
  },
  watch: {
    show() {
      if (this.show) {
        this.formErrors = {};
        this.formData = null;
        this.fetchData();
      }
    },
  },
  methods: {
    fetchData() {
      restGet(`/api/notes/${this.noteId}`, (r) => (this.loading = r)).then(
        (res) => {
          const { updatedAt, ...rest } = res.note.noteContent;
          this.formData = rest;
        }
      );
    },

    processForm() {
      restPostMultiplePartForm(
        `/api/notes/${this.noteId}`,
        this.formData,
        (r) => (this.loading = r)
      )
        .then((res) => {
          this.$emit("updated");
          this.show = false;
          this.$router.push({
            name: "noteShow",
            params: { noteId: res.noteId },
          });
        })
        .catch((res) => (this.formErrors = res));
    },
  },
};
</script>
