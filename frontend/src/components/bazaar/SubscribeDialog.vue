<template>
      <h3>Add to my learning</h3>
      <p v-if="!user">Please login first</p>
      <form v-else @submit.prevent.once="processForm">
        <TextInput
          scopeName="subscription"
          field="dailyTargetOfNewNotes"
          v-model="formData.dailyTargetOfNewNotes"
          :errors="formErrors.dailyTargetOfNewNotes"
        />
        <input type="submit" value="Submit" class="btn btn-primary" />
      </form>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import { restPostMultiplePartForm } from "../../restful/restful";

export default {
  props: { notebook: Object, user: Object },
  components: { TextInput },
  data() {
    return {
      formData: { dailyTargetOfNewNotes: 5 },
      formErrors: {},
    };
  },

  methods: {
    processForm() {
      restPostMultiplePartForm(
        `/api/subscriptions/notebooks/${this.notebook.id}/subscribe`,
        this.formData,
        (r) => (this.loading = r)
      )
        .then((res) => {
          this.$router.push({ name: "notebooks" });
        })
        .catch((res) => (this.formErrors = res));
    },
  },
};
</script>
