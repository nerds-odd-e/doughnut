<template>
  <input type="submit" name="submit" value="Keep for repetition" class="btn btn-primary"
      v-on:click="$emit('doInitialReview', false)"/>
  <input type="submit" name="skip" value="Skip repetition" class="btn btn-secondary"
      v-on:click="$emit('doInitialReview', true)">
</template>

<script>
export default {
  name: 'InitialReviewButtons',
  emits: [ 'doInitialReview' ],
}

</script>
