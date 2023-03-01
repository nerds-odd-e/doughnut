<template>
  <InputWithType v-bind="{ scopeName, field, title, errors, hint }">
    <template #label_content v-if="$slots.label_content">
      <slot name="label_content" />
    </template>
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
  </InputWithType>
</template>

<script>
import InputWithType from "./InputWithType.vue";

export default {
  props: {
    modelValue: String,
    scopeName: String,
    field: String,
    title: String,
    hint: String,
    placeholder: { type: String, default: null },
    autofocus: { type: Boolean, default: false },
    errors: Object,
  },
  components: {
    InputWithType,
  },
  emits: ["update:modelValue", "blur"],
};
</script>
