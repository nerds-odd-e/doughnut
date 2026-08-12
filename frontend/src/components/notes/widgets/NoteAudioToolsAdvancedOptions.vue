<template>
  <div class="advanced-options">
    <div class="input-group">
      <label for="processingInstructions">Processing Instructions:</label>
      <input
        id="processingInstructions"
        v-model="processingInstructions"
        type="text"
        placeholder="Enter additional processing instructions..."
        class="processing-input"
      />
    </div>
    <FullScreen>
      <div v-if="errors" class="fullscreen-error">
        {{ Object.values(errors)[0] }}
      </div>
    </FullScreen>
  </div>
</template>

<script setup lang="ts">
import FullScreen from "@/components/common/FullScreen.vue"

defineProps<{
  errors?: Record<string, string | undefined>
}>()

const processingInstructions = defineModel<string>("processingInstructions", {
  required: true,
})
</script>

<style scoped>
.advanced-options {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-group label {
  color: #a0aec0;
  font-size: 14px;
}

.processing-input {
  padding: 8px 12px;
  border-radius: 4px;
  border: 1px solid #4299e1;
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
  font-size: 14px;
}

.processing-input:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.5);
}

.fullscreen-error {
  color: #fc8181;
  font-size: 14px;
  text-align: center;
  max-width: 80%;
}
</style>
