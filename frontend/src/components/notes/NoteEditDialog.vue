<template>
  <form @submit.prevent.once="processForm">
    <NoteFormBody v-if="!!formData" v-model="formData" :errors="formErrors" />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script>
import NoteFormBody from "./NoteFormBody.vue";
import { storedApiGetNoteAndItsChildren, storedApiUpdateNote } from "../../storedApi";

export default {
  name: "NoteEditDialog",
  components: {
    NoteFormBody,
  },
  props: { noteId: [String, Number] },
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
      storedApiGetNoteAndItsChildren(this.$store, this.noteId)
      .then((res) => {
          const { updatedAt, ...rest } = res.notes[0].noteContent
          this.formData = rest
        }
      )
      .finally(() => this.loading = false)
    },

    processForm() {
      this.loading = true
      storedApiUpdateNote(this.$store, this.noteId, this.formData)
      .then(() => {
        this.$emit("done");
      })
      .catch((res) => (this.formErrors = res))
      .finally(() => this.loading = false)
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
