<template>
  <h3>Edit notebook settings</h3>
  <form @submit.prevent.once="processForm">
    <CheckInput
      scope-name="notebook"
      field="skipReviewEntirely"
      v-model="formData.skipReviewEntirely"
      :errors="formErrors.skipReviewEntirely"
    />
    <input type="submit" value="Update" class="btn btn-primary" />
  </form>
</template>

<script>
import useLoadingApi from "../../managedApi/useLoadingApi";
import CheckInput from "../form/CheckInput.vue";

export default {
  setup() {
    return useLoadingApi({ hasFormError: true });
  },
  props: { notebook: Object },
  components: { CheckInput },
  data() {
    const { skipReviewEntirely } = this.notebook;
    return {
      formData: { skipReviewEntirely },
    };
  },

  methods: {
    processForm() {
      this.api.notebookMethods
        .updateNotebookSettings(this.notebook.id, this.formData)
        .then(() => {
          this.$router.push({ name: "notebooks" });
        });
    },
  },
};
</script>
