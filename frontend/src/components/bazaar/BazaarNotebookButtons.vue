<template>
  <div class="btn-group btn-group-sm">
    <SubscribeButton
      v-if="!notebook.skipReviewEntirely"
      v-bind="{ notebook, user }"
    />
  </div>
</template>

<script setup>
import SubscribeButton from "./SubscribeButton.vue";
const props = defineProps({ notebook: Object, user: Object });
</script>
