<template>
  <input
    type="submit"
    name="submit"
    value="Keep for repetition"
    class="daisy-btn daisy-btn-primary"
    @click.once="$emit('assimilate', false)"
  />
  <input
    type="submit"
    name="skip"
    value="Skip repetition"
    class="daisy-btn daisy-btn-secondary"
    @click.once="$emit('assimilate', true)"
  />
</template>

<script>
export default {
  emits: ["assimilate"],
}
</script>
