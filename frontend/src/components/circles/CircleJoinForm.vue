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
import TextInput from "@/components/form/TextInput.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"

export default defineComponent({
  setup() {
    return useLoadingApi()
  },
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
    processForm() {
      this.managedApi.restCircleController
        .joinCircle(this.formData)
        .then((res) => {
          this.$router.push({
            name: "circleShow",
            params: { circleId: res.id },
          })
        })
        .catch((err) => (this.errors = err as Record<string, string>))
    },
  },
})
</script>
