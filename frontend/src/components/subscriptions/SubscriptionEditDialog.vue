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
import type { Subscription, SubscriptionDTO } from "@generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import TextInput from "../form/TextInput.vue"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
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
      formData: { dailyTargetOfNewNotes } as SubscriptionDTO,
      errors: {} as Record<string, string>,
    }
  },

  methods: {
    processForm() {
      this.managedApi.services
        .update({
          subscription: this.subscription.id,
          requestBody: this.formData,
        })
        .then(() => {
          this.$router.push({ name: "notebooks" })
        })
        .catch((err) => (this.errors = err as Record<string, string>))
    },
  },
})
</script>
