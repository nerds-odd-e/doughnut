<template>
  <form @submit.prevent.once="processForm">
    <NoteFormBody v-if="!!formData" v-model="formData" :errors="formErrors" />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script>
import NoteFormBody from "./NoteFormBody.vue";
import { restGet, restPostMultiplePartForm } from "../../restful/restful";

export default {
  name: "NoteEditDialog",
  components: {
    NoteFormBody,
  },
  props: { noteId: Number },
  emits: ["done"],
  data() {
    return {
      formData: null,
      formErrors: {},
      loading: true,
    };
  },

  methods: {
    fetchData() {
      this.loading = true
      restGet(`/api/notes/${this.noteId}`).then(
        (res) => {
          const { updatedAt, ...rest } = res.note.noteContent
          this.formData = rest
        }
      )
      .finally(() => this.loading = false)
    },

    processForm() {
      restPostMultiplePartForm(
        `/api/notes/${this.noteId}`,
        this.formData,
        (r) => (this.loading = r)
      )
        .then((res) => {
          this.$store.commit("loadNotes", [res]);
          this.$emit("done");
        })
        .catch((res) => (this.formErrors = res));
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
