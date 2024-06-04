<template>
  <h3>Assessment</h3>
  <p v-if="!loggedIn">Please login first</p>
  <form v-else @submit.prevent.once="processForm">
    <p>Do you want to generate assessment questions?</p>
    <input type="button" value="Generate" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Notebook, SubscriptionDTO } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    notebook: { type: Object as PropType<Notebook>, required: true },
    loggedIn: Boolean,
  },
  emits: ["closeDialog"],
  data() {
    return {
      formData: { dailyTargetOfNewNotes: 5 } as SubscriptionDTO,
      errors: {},
    };
  },

  methods: {
    processForm() {
      this.managedApi.restSubscriptionController
        .createSubscription(this.notebook.id, this.formData)
        .then(() => {
          this.$emit("closeDialog");
          this.$router.push({ name: "notebooks" });
        })
        .catch((res) => (this.errors = res));
    },
  },
});
</script>
