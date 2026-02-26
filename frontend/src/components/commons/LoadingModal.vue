<template>
  <Overlay v-if="show" class="loading-modal-mask" centered dark :z-index="10000">
    <div class="loading-modal-content">
      <div class="daisy-loading daisy-loading-spinner daisy-loading-lg"></div>
      <p class="loading-message">{{ message }}</p>
    </div>
  </Overlay>
</template>

<script setup lang="ts">
import Overlay from "./Overlay.vue"

interface Props {
  show: boolean
  message?: string
}

withDefaults(defineProps<Props>(), {
  message: "Processing...",
})
</script>

<style scoped>
.loading-modal-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.daisy-loading-spinner {
  color: white;
}

.loading-message {
  color: white;
  font-size: 1.125rem;
  font-weight: 500;
  margin: 0;
}
</style>
