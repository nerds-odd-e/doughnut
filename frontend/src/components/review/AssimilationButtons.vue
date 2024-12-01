<template>
  <input
    type="submit"
    name="submit"
    value="Keep for repetition"
    class="btn btn-primary"
    @click.once="$emit('assimilate', false)"
  />
  <input
    type="submit"
    name="skip"
    value="Skip repetition"
    class="btn btn-secondary"
    @click.once="$emit('assimilate', true)"
  />
</template>

<script>
export default {
  emits: ["assimilate"],
}
</script>
