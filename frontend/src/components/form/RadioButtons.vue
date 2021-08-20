<template>
  <InputWithType v-bind="{scopeName, field, errors}">
      <output :id="`${scopeName}-${field}`" role="radiogroup" class="filter-switch">
        <span v-for="option in options" :key="option.value" class="filter-switch-item">
          <input type="radio" :value="option.value" :id="`${scopeName}-${option.value}`" v-model="modelValue">
          <label :for="`${scopeName}-${option.value}`">
            <slot name="labelAddition" :value="option.value"/>
            {{option.label}}
          </label>
        </span>
      </output>
  </InputWithType>
</template>

<script>
import InputWithType from "./InputWithType.vue"
export default {
  props: {modelValue: String, scopeName: String, field: String, options: Array, errors: Object},
  emits: ['update:modelValue'],
  components: { InputWithType },
  watch: {
    modelValue() {
      this.$emit('update:modelValue', this.modelValue)
    }
  }
}
</script>

<style scoped>
.filter-switch label {
  font-size: small;
  cursor: pointer;
  margin-right: 5px;
  border-radius: 5px;
  padding: 2px;
  border: solid 1px #32612D; }

.filter-switch .filter-switch-item input {
  display: none; }

.filter-switch .filter-switch-item input:checked + label {
  color: white;
  background-color: #03AC13; }

.filter-switch .filter-switch-item input:not(:checked) + label {
  --bg-opacity: 0;
  box-shadow: none; }

</style>