<template>
  <h3>Add to my learning</h3>
  <p v-if="!loggedIn">Please login first</p>
  <form v-else @submit.prevent.once="processForm">
    <TextInput
      scope-name="subscription"
      field="dailyTargetOfNewNotes"
      v-model="formData.dailyTargetOfNewNotes"
      :errors="errors['dailyTargetOfNewNotes']"
    />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import TextInput from "../form/TextInput.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import asPopup from "../commons/Popups/asPopup";

export default defineComponent({
  setup() {
    return { ...useLoadingApi(), ...asPopup() };
  },
  props: {
    notebook: { type: Object as PropType<Generated.Notebook>, required: true },
    loggedIn: Boolean,
  },
  emits: ["closeDialog"],
  components: { TextInput },
  data() {
    return {
      formData: { dailyTargetOfNewNotes: 5 } as Generated.Subscription,
      errors: {},
    };
  },

  methods: {
    processForm() {
      this.api.subscriptionMethods
        .subscribe(this.notebook.id, this.formData)
        .then(() => {
          this.$emit("closeDialog");
          this.$router.push({ name: "notebooks" });
        })
        .catch((res) => (this.errors = res));
    },
  },
});
</script>
