<template>
  <InputWithType v-bind="{ scopeName, field, errors, beforeLabel: true }">
    <input
      type="checkbox"
      :class="`form-check-input ${!!errors ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="field"
      :checked="modelValue"
      @change="$emit('update:modelValue', $event.target.checked)"
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
    errors: Object,
  },
  emits: ["update:modelValue"],
  components: { InputWithType },
};
</script>
