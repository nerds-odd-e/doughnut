<template>
  <input
    type="submit"
    name="submit"
    value="Keep for repetition"
    class="daisy-btn daisy-btn-primary"
    @click="$emit('assimilate', false)"
  />
  <input
    type="submit"
    name="skip"
    value="Skip repetition"
    class="daisy-btn daisy-btn-secondary"
    @click="$emit('assimilate', true)"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue"

export default defineComponent({
  emits: ["assimilate"],
})
</script>
