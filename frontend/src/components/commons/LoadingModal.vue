<template>
  <Teleport v-if="show" to="body">
    <div class="loading-modal-mask">
      <div class="loading-modal-content">
        <div class="daisy-loading daisy-loading-spinner daisy-loading-lg"></div>
        <p class="loading-message">{{ message }}</p>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
interface Props {
  show: boolean
  message?: string
}

withDefaults(defineProps<Props>(), {
  message: "Processing...",
})
</script>

<style scoped>
.loading-modal-mask {
  position: fixed;
  z-index: 10000;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
}

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
