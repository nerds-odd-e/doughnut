<template>
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
    @blur="$emit('blur', $event)"
  />
</template>

<script>
export default {
  props: {
    modelValue: String,
    scopeName: String,
    field: String,
    title: String,
    hint: String,
    placeholder: { type: String, default: null },
    autofocus: { type: Boolean, default: false },
    errors: String,
  },
  emits: ["update:modelValue", "blur"],
};
</script>
