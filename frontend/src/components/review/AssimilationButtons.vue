<template>
  <input
    type="submit"
    name="submit"
    value="Keep for repetition"
    class="btn btn-primary"
    @click.once="$emit('doOnboarding', false)"
  />
  <input
    type="submit"
    name="skip"
    value="Skip repetition"
    class="btn btn-secondary"
    @click.once="$emit('doOnboarding', true)"
  />
</template>

<script>
export default {
  emits: ["doOnboarding"],
}
</script>
