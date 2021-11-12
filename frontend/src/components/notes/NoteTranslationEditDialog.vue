<template>
  <h3>
    {{ EDIT_TRANSLATION_LABEL }} <em>{{ title }}</em>
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
import StringConstants from "../../constants/labels";
import { restPostMultiplePartForm } from "../../restful/restful"; 

export default {
  components: { NoteTranslationEditForm },
  props: {
    noteId: [String, Number],
    title: String,
    description: String
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
    fetchData() {},
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
          this.$popups.alert(StringConstants.TRANSACTION_SUCCESS_TITLE)
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
  created() {
    this.EDIT_TRANSLATION_LABEL = StringConstants.EDIT_TRANSLATION_LABEL
  },
};
</script>
