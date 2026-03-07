<template>
  <input
    type="submit"
    name="submit"
    value="Keep for recall"
    class="daisy-btn daisy-btn-primary"
    data-test="keep-for-recall"
    :disabled="disabled || keepForRecallDisabled"
    @click="$emit('assimilate', false)"
  />
  <input
    type="submit"
    name="skip"
    value="Skip recall"
    class="daisy-btn daisy-btn-secondary"
    :disabled="disabled"
    @click="$emit('assimilate', true)"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue"

export default defineComponent({
  props: {
    disabled: {
      type: Boolean,
      default: false,
    },
    keepForRecallDisabled: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["assimilate"],
})
</script>
