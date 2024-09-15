<template>
  <InputWithType v-bind="{ scopeName, field, errorMessage }">
    <input
      :class="`file-input-control form-control ${!!errorMessage ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      type="file"
      :name="field"
      @change="update(($event.target as HTMLInputElement).files![0])"
      :placeholder="placeholder"
      :autofocus="autofocus"
      autocomplete="off"
      autocapitalize="off"
      :accept="accept"
    />
  </InputWithType>
</template>

<script setup lang="ts">
import InputWithType from "./InputWithType.vue"

defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  accept: String,
  placeholder: { type: String, default: null },
  autofocus: { type: Boolean, default: false },
  errorMessage: String,
})

const emit = defineEmits(["update:modelValue"])

const update = (file: File | undefined) => {
  emit("update:modelValue", file)
}
</script>
