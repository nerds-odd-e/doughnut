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

<script setup>
import { defineEmit } from "@vue/runtime-core"
import InputWithType from "./InputWithType.vue"
  const props = defineProps({modelValue: String, scopeName: String, field: String, placeholder: String, autofocus: Boolean, errors: Object})
  const emit = defineEmit(['update:modelValue'])

</script>
