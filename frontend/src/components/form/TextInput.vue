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
    title: String,
    placeholder: { type: String, default: null },
    autofocus: { type: Boolean, default: false },
    errors: Object,
    onBlur: {type: Function},
  },
  components: {
    InputWithType
  },
  emits: ["update:modelValue"],
}
</script>
