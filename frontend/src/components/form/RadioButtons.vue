<template>
  <InputWithType v-bind="{ scopeName, field, errors }">
    <output
      :id="`${scopeName}-${field}`"
      role="radiogroup"
      class="btn-group filter-switch"
    >
      <div
        v-for="option in options"
        :key="option.value"
        class="filter-switch-item"
      >
        <input
          class="btn-check"
          type="radio"
          :value="option.value"
          :id="`${scopeName}-${option.value}`"
          :modelValue="modelValue"
          @update:modelValue="$emit('update:modelValue', $event)"
        />
        <label
          role="button"
          :title="option.title"
          class="btn btn-outline-primary text-nowrap"
          :for="`${scopeName}-${option.value}`"
        >
          <slot name="labelAddition" :value="option.value" />
          {{ option.label }}
        </label>
      </div>
    </output>
  </InputWithType>
</template>

<script>
import InputWithType from "./InputWithType.vue";

export default {
  props: {
    modelValue: String,
    scopeName: String,
    field: String,
    options: Array,
    errors: Object,
  },
  emits: ["update:modelValue"],
  components: { InputWithType },
};
</script>

<style scoped>
.btn-group {
  flex-wrap: wrap;
}

label {
  font-size: small;
}
</style>
