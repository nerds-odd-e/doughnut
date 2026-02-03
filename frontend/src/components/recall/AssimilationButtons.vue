<template>
  <input
    type="submit"
    name="submit"
    value="Keep for repetition"
    class="daisy-btn daisy-btn-primary"
    data-test="keep-for-repetition"
    :disabled="disabled"
    @click="$emit('assimilate', false)"
  />
  <input
    type="submit"
    name="skip"
    value="Skip repetition"
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
  },
  emits: ["assimilate"],
})
</script>
