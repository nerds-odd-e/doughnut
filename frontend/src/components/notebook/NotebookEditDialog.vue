<template>
      <h3>Edit notebook settings</h3>
      <form @submit.prevent.once="processForm">
        <CheckInput
          scopeName="notebook"
          field="skipReviewEntirely"
          v-model="formData.skipReviewEntirely"
          :errors="formErrors.skipReviewEntirely"
        />
        <input type="submit" value="Update" class="btn btn-primary" />
      </form>
</template>

<script>
import CheckInput from "../form/CheckInput.vue";
import api from  "../../managedApi/api";

export default {
  props: { notebook: Object },
  components: { CheckInput },
  data() {
    const { skipReviewEntirely } = this.notebook;
    return {
      formData: { skipReviewEntirely },
      formErrors: {},
    };
  },

  methods: {
    processForm() {
      this.loading = true
      api(this).updateNotebookSettings(this.notebook.id, this.formData)
        .then((res) => {
          this.$router.push({ name: "notebooks" });
        })
        .catch((res) => (this.formErrors = res))
        .finally(()=>this.loading = false);
    },
  },
};
</script>
