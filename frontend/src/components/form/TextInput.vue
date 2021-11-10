<template>
  <InputWithType v-bind="{ scopeName, field, title, errors }">
    <input
      :class="`text-input-control form-control ${!!errors ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="field"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      :placeholder="placeholder"
      :autofocus="autofocus"
      autocomplete="off"
      autocapitalize="off"
    />
  </InputWithType>
</template>

<script setup>
import InputWithType from "./InputWithType.vue";
const props = defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  title: String,
  placeholder: { type: String, default: null },
  autofocus: { type: Boolean, default: false },
  errors: Object,
});
const emit = defineEmits(["update:modelValue"]);
</script>
