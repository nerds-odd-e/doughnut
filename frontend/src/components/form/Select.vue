<template>
  <InputWithType v-bind="{ scopeName, field, errors }">
    <select
      :class="`select-control form-control ${!!errors ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="scopeName"
      @input="$emit('update:modelValue', $event.target.value)"
      @change="(e) => onchange(scopeName, e.target.value)"
    >
      <option
        class="options"
        v-for="option in options"
        :key="option.key"
        :value="option.value"
      >
        {{ option.label }}
      </option>
    </select>
  </InputWithType>
</template>

<script setup>
import InputWithType from "./InputWithType.vue";

const props = defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  options: Array,
  errors: Object,
  onchange: Function,
});

const emit = defineEmits(["update:modelValue"]);
</script>
