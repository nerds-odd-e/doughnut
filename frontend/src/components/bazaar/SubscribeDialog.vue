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
import type { Notebook, SubscriptionDTO } from "generated/backend"
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
