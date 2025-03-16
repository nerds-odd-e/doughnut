<template>
  <InputWithType v-bind="{ scopeName, field, errorMessage }">
    <input
      :class="`daisy:file-input daisy:w-full ${!!errorMessage ? 'daisy:file-input-error' : ''}`"
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
