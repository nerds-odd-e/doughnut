<template>
  <InputWithType v-bind="{scopeName, field, errors}">
      <input
              class="text-input-control form-control put_error_class_here"
              :id="`${scopeName}-${field}`"
              :name="field"
              :value="modelValue"
              @input="$emit('update:modelValue', $event.target.value)"
              :placeholder="placeholder"
              :autofocus="autofocus"
              autocomplete="off"
              autocapitalize="off"
      />
  </InputWithType>
</template>

<script>
  import InputWithType from "./InputWithType.vue"
  import { computed } from 'vue'
  export  default {
    props: {modelValue: String, scopeName: String, field: String, placeholder: String, autofocus: Boolean, errors: Object},
    emits: ['update:modelValue'],
    components: {InputWithType},
  }

</script>
