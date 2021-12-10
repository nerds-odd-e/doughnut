<template>
  <InputWithType v-bind="{ scopeName, field, errors }">
    <textarea
      :class="`area-control form-control ${!!errors ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="field"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      :placeholder="placeholder"
      :autofocus="autofocus"
      autocomplete="off"
      autocapitalize="off"
      rows="8"
      @blur="onBlur"
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
    placeholder: { type: String, default: null },
    autofocus: { type: Boolean, default: false },
    errors: Object,
    onBlur: {type: Function},
  },
  emits: ["update:modelValue"],
  components: { InputWithType },
};
</script>
