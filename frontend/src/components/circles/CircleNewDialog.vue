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
import type { Circle } from "@generated/backend"
import { CircleController } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { defineComponent } from "vue"
import TextInput from "../form/TextInput.vue"

export default defineComponent({
  emits: ["closeDialog"],
  components: { TextInput },
  data() {
    return {
      formData: {} as Circle,
      errors: {},
    }
  },

  methods: {
    async processForm() {
      const { data: newCircle, error } = await apiCallWithLoading(() =>
        CircleController.createCircle({
          body: this.formData,
        })
      )
      if (!error) {
        this.$emit("closeDialog")
        await this.$router.push({
          name: "circleShow",
          params: { circleId: newCircle!.id },
        })
      } else {
        // Error is handled by global interceptor (toast notification)
        // Extract field-level errors if available (for 400 validation errors)
        const errorObj = toOpenApiError(error)
        this.errors = errorObj.errors || {}
      }
    },
  },
})
</script>
