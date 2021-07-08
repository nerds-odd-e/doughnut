<template>
  <InputWithType v-bind="{scopeName, errors}">
      <span class="form-check form-check-inline" v-for="option in options" :key="option.value">
        <input class="form-check-input" type="radio" :value="option.value" :id="`${scopeName}-${option.value}`" v-model="modelValue">
        <label class="form-check-label" :for="`${scopeName}-${option.value}`">{{option.label}}</label>
      </span>
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
