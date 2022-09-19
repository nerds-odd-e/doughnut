<template>
  <h3>Edit notebook settings</h3>
  <form @submit.prevent.once="processForm">
    <CheckInput
      scope-name="notebook"
      field="skipReviewEntirely"
      v-model="formData.skipReviewEntirely"
      :errors="errors.skipReviewEntirely"
    />
    <input type="submit" value="Update" class="btn btn-primary" />
  </form>
</template>

<script>
import useLoadingApi from "../../managedApi/useLoadingApi";
import CheckInput from "../form/CheckInput.vue";

export default {
  setup() {
    return useLoadingApi();
  },
  props: { notebook: Object },
  components: { CheckInput },
  data() {
    const { skipReviewEntirely } = this.notebook;
    return {
      formData: { skipReviewEntirely },
      errors: {},
    };
  },

  methods: {
    processForm() {
      this.api.notebookMethods
        .updateNotebookSettings(this.notebook.id, this.formData)
        .then(() => {
          this.$router.push({ name: "notebooks" });
        })
        .catch((err) => (this.errors = err));
    },
  },
};
</script>
