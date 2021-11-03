<template>
  <InputWithType v-bind="{ scopeName, field, errors }">
    <output
      :id="`${scopeName}-${field}`"
      role="radiogroup"
      class="btn-group filter-switch"
    >
      <template
        v-for="option in options"
        :key="option.value"
        class="filter-switch-item"
      >
        <input
          class="btn-check"
          type="radio"
          :value="option.value"
          :id="`${scopeName}-${option.value}`"
          v-model="modelValue"
        />
        <label role="button" :title ="option.title" class="btn btn-outline-primary text-nowrap" :for="`${scopeName}-${option.value}`">
          <slot name="labelAddition" :value="option.value" />
          {{ option.label }}
        </label>
      </template>
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
  watch: {
    modelValue() {
      this.$emit("update:modelValue", this.modelValue);
    },
  },
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
