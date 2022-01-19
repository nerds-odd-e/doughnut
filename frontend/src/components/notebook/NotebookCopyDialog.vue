<template>
      <h3>Copy notebook</h3>
      <form @submit.prevent.once="processForm">
        <input type="submit" value="Copy" class="btn btn-primary"/>
      </form>
</template>

<script>
import {restPost} from "../../restful/restful";

export default {
  props: {notebook: Object},
  data() {
    const {skipReviewEntirely} = this.notebook;
    return {
      formData: {skipReviewEntirely},
      formErrors: {},
    };
  },

  methods: {
    processForm() {
      restPost(
          `/api/notebooks/${this.notebook.id}/copy`,
          {},
          (r) => {
          }
      ).then((res) => {
        this.$router.push({name: "notebooks"});
      }).catch((res) => (this.formErrors = res));
    },
  },
};
</script>
