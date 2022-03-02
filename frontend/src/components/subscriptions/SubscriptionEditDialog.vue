<template>
      <h3>Edit subscription</h3>
      <form @submit.prevent.once="processForm">
        <TextInput
          scopeName="subscription"
          field="dailyTargetOfNewNotes"
          v-model="formData.dailyTargetOfNewNotes"
          :errors="formErrors.dailyTargetOfNewNotes"
        />
        <input type="submit" value="Update" class="btn btn-primary" />
      </form>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import { restPostMultiplePartForm } from "../../restful/restful";

export default {
  props: { subscription: Object },
  components: { TextInput },
  data() {
    const { dailyTargetOfNewNotes } = this.subscription;
    return {
      formData: { dailyTargetOfNewNotes },
      formErrors: {},
    };
  },

  methods: {
    processForm() {
      this.loading = true
      restPostMultiplePartForm(
        `/api/subscriptions/${this.subscription.id}`,
        this.formData,
      )
        .then((res) => {
          this.$router.push({ name: "notebooks" });
        })
        .catch((res) => (this.formErrors = res))
        .finally(() => this.loading = false)
    },
  },
};
</script>
