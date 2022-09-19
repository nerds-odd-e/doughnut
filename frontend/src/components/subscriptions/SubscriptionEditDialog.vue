<template>
  <h3>Edit subscription</h3>
  <form @submit.prevent.once="processForm">
    <TextInput
      scope-name="subscription"
      field="dailyTargetOfNewNotes"
      v-model="formData.dailyTargetOfNewNotes"
      :errors="errors.dailyTargetOfNewNotes"
    />
    <input type="submit" value="Update" class="btn btn-primary" />
  </form>
</template>

<script>
import useLoadingApi from "../../managedApi/useLoadingApi";
import TextInput from "../form/TextInput.vue";

export default {
  setup() {
    return useLoadingApi();
  },
  props: { subscription: Object },
  components: { TextInput },
  data() {
    const { dailyTargetOfNewNotes } = this.subscription;
    return {
      formData: { dailyTargetOfNewNotes },
      errors: {},
    };
  },

  methods: {
    processForm() {
      this.api.subscriptionMethods
        .updateSubscription(this.subscription.id, this.formData)
        .then(() => {
          this.$router.push({ name: "notebooks" });
        })
        .catch((err) => (this.errors = err));
    },
  },
};
</script>
