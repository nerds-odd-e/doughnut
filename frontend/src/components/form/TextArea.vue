<template>
  <InputWithType v-bind="{ class: $attrs.class, scopeName, field, errors }">
    <textarea
      :class="`area-control form-control ${!!errors ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="field"
      :value="modelValue"
      @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
      :placeholder="placeholder"
      :autofocus="autofocus"
      autocomplete="off"
      autocapitalize="off"
      :rows="rows"
      @blur="emit('blur', $event)"
      ref="input"
    />
  </InputWithType>
</template>

<script setup lang="ts">
import { ref } from "vue"
import InputWithType from "./InputWithType.vue"

defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  placeholder: { type: String, default: null },
  autofocus: { type: Boolean, default: false },
  rows: { type: Number, default: 8 },
  errors: Object,
})

const emit = defineEmits(["update:modelValue", "blur"])

const input = ref<HTMLTextAreaElement | null>(null)

const focus = () => {
  if(input.value === null) return
  input.value.focus()
}

defineExpose({
  focus
})
</script>
