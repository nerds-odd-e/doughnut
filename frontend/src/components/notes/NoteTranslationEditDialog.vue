<template>
  <h3>
     Edit translation for <em>{{ formData?.title }}</em>
  </h3>
  <form @submit.prevent="processForm">
    <NoteTranslationEditForm
      scopeName="review-setting"
      v-model="formData"
      :errors="formErrors"
    />
    <input type="submit" value="Update" class="btn btn-primary" />
  </form>
</template>

<script>
import NoteTranslationEditForm from "./NoteTranslationEditForm.vue";
import { restPostMultiplePartForm } from "../../restful/restful";
import { storedApiGetNoteAndItsChildren } from "../../storedApi";

export default {
  components: { NoteTranslationEditForm },
  props: {
    noteId: [String, Number],
  },
  emits: ["done"],
  data() {
    return {
      formData: {},
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
      const note = this.$store.getters.getNoteById(this.noteId)
      const noteContent = {...note.noteContent}
      delete noteContent.updatedAt

      restPostMultiplePartForm(
        `/api/notes/${this.noteId}`,
        {...noteContent, ...this.formData},
        (result) => (this.loading = result)
      ).then((res) => {
          this.$store.commit("loadNotes", [res])
          this.$emit("done")
        })
        .catch((res) => {
            this.formErrors = res.message
            alert(res.message)
        });
    },
  },
  mounted() {
    this.fetchData()
  },
};
</script>
