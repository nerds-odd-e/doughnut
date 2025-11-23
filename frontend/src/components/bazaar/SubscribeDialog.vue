<template>
  <div class="daisy-card daisy-w-96 daisy-shadow-xl">
    <div class="daisy-card-body">
      <h3 class="daisy-card-title">Add to my learning</h3>
      <p v-if="!loggedIn">Please login first</p>
      <form v-else @submit.prevent.once="processForm">
        <TextInput
          scope-name="subscription"
          field="dailyTargetOfNewNotes"
          v-model="formData.dailyTargetOfNewNotes"
          :error-message="errors['dailyTargetOfNewNotes']"
        />
        <input
          type="submit"
          value="Submit"
          class="daisy-btn daisy-btn-primary daisy-mt-4"
        />
      </form>
    </div>
  </div>
</template>

<script lang="ts">
import TextInput from "@/components/form/TextInput.vue"
import type { Notebook, SubscriptionDto } from "@generated/backend"
import { createSubscription } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import type { PropType } from "vue"
import { defineComponent } from "vue"

export default defineComponent({
  props: {
    notebook: { type: Object as PropType<Notebook>, required: true },
    loggedIn: Boolean,
  },
  emits: ["closeDialog"],
  components: { TextInput },
  data() {
    return {
      formData: { dailyTargetOfNewNotes: 5 } as SubscriptionDto,
      errors: {} as Record<string, string>,
    }
  },

  methods: {
    async processForm() {
      const { error } = await createSubscription({
        path: { notebook: this.notebook.id },
        body: this.formData,
      })
      if (!error) {
        this.$emit("closeDialog")
        this.$router.push({ name: "notebooks" })
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
