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
      this.$popups.alert(StringConstants.TRANSACTION_SUCCESS_TITLE);
    },
  },
  mounted() {
    this.fetchData();
  },
  created() {
    this.EDIT_TRANSLATION_LABEL = StringConstants.EDIT_TRANSLATION_LABEL;
  },
};
</script>
