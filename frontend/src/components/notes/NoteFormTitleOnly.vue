<template>
  <TextInput
    scope-name="note"
    field="title"
    :model-value="modelValue"
    :error-message="errorMessage"
    :initial-select-all="true"
    v-focus
    @update:model-value="$emit('update:modelValue', $event)"
    @blur="emit('blur')"
    @focus="emit('focus')"
  >
    <template #input_append v-if="$slots.input_append">
      <slot name="input_append" />
    </template>
  </TextInput>
</template>

<script setup lang="ts">
import TextInput from "../form/TextInput.vue"

defineProps<{
  modelValue?: string
  errorMessage?: string
}>()

const emit = defineEmits<{
  "update:modelValue": [value: string]
  blur: []
  focus: []
}>()
</script>
