<template>
  <h3>Creating a new circle</h3>
  <form @submit.prevent.once="processForm">
    <TextInput
      scope-name="circle"
      field="name"
      v-model="formData.name"
      :error-message="errors['name']"
    />
    <input type="submit" value="Submit" class="daisy-btn daisy-btn-primary" />
  </form>
</template>

<script lang="ts">
import type { Circle } from "generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { defineComponent } from "vue"
import TextInput from "../form/TextInput.vue"

export default defineComponent({
  setup() {
    return { ...useLoadingApi() }
  },
  emits: ["closeDialog"],
  components: { TextInput },
  data() {
    return {
      formData: {} as Circle,
      errors: {},
    }
  },

  methods: {
    processForm() {
      this.managedApi.restCircleController
        .createCircle(this.formData)
        .then((res) => {
          this.$emit("closeDialog")
          this.$router.push({
            name: "circleShow",
            params: { circleId: res.id },
          })
        })
        .catch((err) => (this.errors = err))
    },
  },
})
</script>
