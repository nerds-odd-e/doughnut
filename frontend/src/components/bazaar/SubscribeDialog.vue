<template>
  <h3>Add to my learning</h3>
  <p v-if="!loggedIn">Please login first</p>
  <form v-else @submit.prevent.once="processForm">
    <TextInput
      scope-name="subscription"
      field="dailyTargetOfNewNotes"
      v-model="formData.dailyTargetOfNewNotes"
      :errors="errors.dailyTargetOfNewNotes"
    />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  props: { notebook: Object, loggedIn: Boolean },
  components: { TextInput },
  data() {
    return {
      formData: { dailyTargetOfNewNotes: 5 },
      errors: {},
    };
  },

  methods: {
    processForm() {
      this.api.subscriptionMethods
        .subscribe(this.notebook.id, this.formData)
        .then((res) => {
          this.$router.push({ name: "notebooks" });
        })
        .catch((res) => (this.errors = res));
    },
  },
};
</script>
