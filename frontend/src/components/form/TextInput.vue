<template>
  <InputWithType v-bind="{ scopeName, field, title, errors, hint }">
    <template #input_prepend v-if="$slots.input_prepend">
      <slot name="input_prepend" />
    </template>
    <TextInputWithoutErrorMessage
      :model-value="modelValue"
      :scope-name="scopeName"
      :field="field"
      :title="title"
      :errors="errors"
      :placeholder="placeholder"
      :autofocus="autofocus"
      @blur="$emit('blur', $event)"
      @update:model-value="$emit('update:modelValue', $event)"
    />
  </InputWithType>
</template>

<script>
import InputWithType from "./InputWithType.vue";
import TextInputWithoutErrorMessage from "./TextInputWithoutErrorMessage.vue";

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
  components: {
    InputWithType,
    TextInputWithoutErrorMessage,
  },
  emits: ["update:modelValue", "blur"],
};
</script>
