<template>
  <InputWithType v-bind="{ scopeName, field, title, errorMessage, hint }">
    <template #input_prepend v-if="$slots.input_prepend">
      <slot name="input_prepend" />
    </template>
    <input
      :class="`daisy-input daisy-input-bordered daisy-w-full ${!!errorMessage ? 'daisy-input-error' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="field"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      :placeholder="placeholder"
      :autofocus="autofocus"
      autocomplete="off"
      autocapitalize="off"
      @blur="$emit('blur')"
      @focus="$emit('focus')"
      :disabled="disabled"
    />
  </InputWithType>
</template>

<script setup lang="ts">
import InputWithType from "./InputWithType.vue"
import { onMounted } from "vue"

const props = defineProps({
  modelValue: { type: [String, Number], required: false },
  scopeName: String,
  field: String,
  title: String,
  hint: String,
  placeholder: { type: String, default: null },
  autofocus: { type: Boolean, default: false },
  errorMessage: String,
  disabled: { type: Boolean, default: false },
  initialSelectAll: { type: Boolean, default: false },
})

defineEmits<{
  "update:modelValue": [value: string]
  blur: []
  focus: []
}>()

onMounted(() => {
  if (props.initialSelectAll && props.modelValue) {
    const input = document.getElementById(
      `${props.scopeName}-${props.field}`
    ) as HTMLInputElement
    if (input) {
      input.select()
    }
  }
})
</script>
