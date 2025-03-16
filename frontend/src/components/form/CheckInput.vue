<template>
  <InputWithType v-bind="{ scopeName, field, errorMessage, beforeLabel: true }">
    <input
      type="checkbox"
      :class="`daisy:checkbox ${!!errorMessage ? 'daisy:checkbox-error' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="field"
      :checked="modelValue"
      :disabled="disabled"
      @change="$emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
    />
  </InputWithType>
</template>

<script setup lang="ts">
import InputWithType from "./InputWithType.vue"

defineProps({
  modelValue: Boolean,
  scopeName: String,
  field: String,
  errorMessage: String,
  disabled: Boolean,
})

defineEmits(["update:modelValue"])
</script>
