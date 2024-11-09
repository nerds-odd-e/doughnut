<template>
  <TextInput
    scope-name="note"
    field="topic"
    :model-value="modelValue"
    :error-message="errorMessage"
    v-focus
    @update:model-value="$emit('update:modelValue', $event)"
    @blur="emit('blur')"
    @focus="emit('focus')"

  />
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
