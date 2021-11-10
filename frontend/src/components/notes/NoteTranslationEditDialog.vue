<template>
  <h3>
    Edit translation for <em>{{ title }}</em>
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

export default {
  components: { NoteTranslationEditForm },
  props: { noteId: [String, Number], title: String, description: String },
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
    processForm() {},
  },
  mounted() {
    this.fetchData();
  },
};
</script>
