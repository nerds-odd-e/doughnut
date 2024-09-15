<template>
  <InputWithType v-bind="{ scopeName, field, title, errorMessage, hint }">
    <template #input_prepend v-if="$slots.input_prepend">
      <slot name="input_prepend" />
    </template>
    <input
      :class="`text-input-control form-control ${!!errorMessage ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="field"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      :placeholder="placeholder"
      :autofocus="autofocus"
      autocomplete="off"
      autocapitalize="off"
      @blur="$emit('blur', $event)"
      :disabled="disabled"
    />
  </InputWithType>
</template>

<script setup lang="ts">
import InputWithType from "./InputWithType.vue"

defineProps({
  modelValue: { type: [String, Number], required: false },
  scopeName: String,
  field: String,
  title: String,
  hint: String,
  placeholder: { type: String, default: null },
  autofocus: { type: Boolean, default: false },
  errorMessage: String,
  disabled: { type: Boolean, default: false },
})
</script>
