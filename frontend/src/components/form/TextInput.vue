<template>
  <InputWithType v-bind="{scopeName, field, errors}">
      <input
              class="text-input-control form-control put_error_class_here"
              :id="`${scopeName}-${field}`"
              :name="field"
              :value="value"
              :placeholder="placeholder"
              :autofocus="autofocus"
              autocomplete="off"
              autocapitalize="off"
      />
  </InputWithType>
</template>

<script setup>
  import InputWithType from "./InputWithType.vue"
  const props = defineProps({scopeName: String, field: String, value: String, placeholder: String, autofocus: Boolean, errors: Object})
</script>
