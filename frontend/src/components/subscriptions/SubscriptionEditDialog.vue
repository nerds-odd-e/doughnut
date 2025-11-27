<template>
  <h3>Edit subscription</h3>
  <form @submit.prevent.once="processForm">
    <TextInput
      scope-name="subscription"
      field="dailyTargetOfNewNotes"
      v-model="formData.dailyTargetOfNewNotes"
      :error-message="errors.dailyTargetOfNewNotes"
    />
    <input type="submit" value="Update" class="daisy-btn daisy-btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue"
import type { Subscription, SubscriptionDto } from "@generated/backend"
import { SubscriptionController } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import TextInput from "../form/TextInput.vue"

export default defineComponent({
  props: {
    subscription: {
      type: Object as PropType<Subscription>,
      required: true,
    },
  },
  components: { TextInput },
  data() {
    const { dailyTargetOfNewNotes } = this.subscription
    return {
      formData: { dailyTargetOfNewNotes } as SubscriptionDto,
      errors: {} as Record<string, string>,
    }
  },

  methods: {
    async processForm() {
      const { error } = await apiCallWithLoading(() =>
        SubscriptionController.updateSubscription({
          path: { subscription: this.subscription.id },
          body: this.formData,
        })
      )
      if (!error) {
        await this.$router.push({ name: "notebooks" })
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
