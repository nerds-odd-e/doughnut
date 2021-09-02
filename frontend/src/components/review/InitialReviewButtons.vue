<template>
  <input
    type="submit"
    name="submit"
    value="Keep for repetition"
    class="btn btn-primary"
    @click.once="$emit('doInitialReview', false)"
  />
  <input
    type="submit"
    name="skip"
    value="Skip repetition"
    class="btn btn-secondary"
    @click.once="$emit('doInitialReview', true)"
  />
</template>

<script>
export default {
  name: "InitialReviewButtons",
  emits: ["doInitialReview"],
};
</script>
