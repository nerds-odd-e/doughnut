<template>
  <h1>Joining a Circle</h1>
  <form @submit.prevent.once="processForm">
    <TextInput
      scope-name="join-circle"
      field="invitationCode"
      v-model="formData.invitationCode"
      :autofocus="true"
      :error-message="errors.invitationCode"
    />
    <input type="submit" value="Join" class="daisy-btn daisy-btn-primary" />
  </form>
</template>

<script lang="ts">
import { defineComponent } from "vue"
import type { Circle, CircleJoiningByInvitation } from "@generated/backend"
import { joinCircle } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import TextInput from "@/components/form/TextInput.vue"

export default defineComponent({
  components: { TextInput },
  props: {
    invitationCode: Number,
  },

  data() {
    return {
      circle: null as Circle | null,
      formData: {
        invitationCode: this.invitationCode?.toString() ?? "",
      } as CircleJoiningByInvitation,
      errors: {} as Record<string, string>,
    }
  },

  methods: {
    async processForm() {
      const { data: joinedCircle, error } = await joinCircle({
        body: this.formData,
      })
      if (!error) {
        this.$router.push({
          name: "circleShow",
          params: { circleId: joinedCircle!.id },
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
