<template>
  <h3>Creating a new circle</h3>
  <form @submit.prevent.once="processForm">
    <TextInput
      scope-name="circle"
      field="name"
      v-model="formData.name"
      :errors="errors['name']"
    />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { Circle } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import TextInput from "../form/TextInput.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  emits: ["closeDialog"],
  components: { TextInput },
  data() {
    return {
      formData: {} as Circle,
      errors: {},
    };
  },

  methods: {
    processForm() {
      this.managedApi.restCircleController
        .createCircle(this.formData)
        .then((res) => {
          this.$emit("closeDialog");
          this.$router.push({
            name: "circleShow",
            params: { circleId: res.id },
          });
        })
        .catch((err) => (this.errors = err));
    },
  },
});
</script>
