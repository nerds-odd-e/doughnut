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
import useLoadingApi from '../../managedApi/useLoadingApi';
import TextInput from "../form/TextInput.vue";

export default {
  setup() {
    return useLoadingApi({hasFormError: true})
  },
  props: { subscription: Object },
  components: { TextInput },
  data() {
    const { dailyTargetOfNewNotes } = this.subscription;
    return {
      formData: { dailyTargetOfNewNotes },
    };
  },

  methods: {
    processForm() {
      this.api.subscriptionMethods.updateSubscription(this.subscription.id, this.formData)
        .then((res) => {
          this.$router.push({ name: "notebooks" });
        })
    },
  },
};
</script>
