<template>
  <h3>Edit notebook settings</h3>
  <form @submit.prevent.once="processForm">
    <CheckInput
      scope-name="notebook"
      field="skipReviewEntirely"
      v-model="formData.skipReviewEntirely"
      :errors="errors.skipReviewEntirely"
    />
    <TextInput
      scope-name="notebook"
      field="numberOfQuestionsInAssessment"
      v-model="formData.numberOfQuestionsInAssessment"
      :errors="errors.numberOfQuestionsInAssessment"
    />
    <input type="submit" value="Update" class="btn btn-primary" />
  </form>
</template>

<script>
import useLoadingApi from "@/managedApi/useLoadingApi";
import CheckInput from "@/components/form/CheckInput.vue";
import TextInput from "../form/TextInput.vue";

export default {
  setup() {
    return useLoadingApi();
  },
  props: { notebook: Object },
  components: { CheckInput, TextInput },
  data() {
    const { skipReviewEntirely, numberOfQuestionsInAssessment } = this.notebook;
    return {
      formData: { skipReviewEntirely, numberOfQuestionsInAssessment },
      errors: {},
    };
  },

  methods: {
    processForm() {
      this.managedApi.restNotebookController
        .update1(this.notebook.id, this.formData)
        .then(() => {
          this.$router.push({ name: "notebooks" });
        })
        .catch((err) => (this.errors = err));
    },
  },
};
</script>
