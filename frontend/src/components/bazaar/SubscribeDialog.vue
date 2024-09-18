<template>
  <h3>Add to my learning</h3>
  <p v-if="!loggedIn">Please login first</p>
  <form v-else @submit.prevent.once="processForm">
    <TextInput
      scope-name="subscription"
      field="dailyTargetOfNewNotes"
      v-model="formData.dailyTargetOfNewNotes"
      :error-message="errors['dailyTargetOfNewNotes']"
    />
    <input type="submit" value="Submit" class="btn btn-primary" />
  </form>
</template>

<script lang="ts">
import TextInput from "@/components/form/TextInput.vue"
import type { Notebook, SubscriptionDTO } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { PropType } from "vue"
import { defineComponent } from "vue"

export default defineComponent({
  setup() {
    return { ...useLoadingApi() }
  },
  props: {
    notebook: { type: Object as PropType<Notebook>, required: true },
    loggedIn: Boolean,
  },
  emits: ["closeDialog"],
  components: { TextInput },
  data() {
    return {
      formData: { dailyTargetOfNewNotes: 5 } as SubscriptionDTO,
      errors: {},
    }
  },

  methods: {
    processForm() {
      this.managedApi.restSubscriptionController
        .createSubscription(this.notebook.id, this.formData)
        .then(() => {
          this.$emit("closeDialog")
          this.$router.push({ name: "notebooks" })
        })
        .catch((res) => (this.errors = res))
    },
  },
})
</script>
