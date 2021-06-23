<template>
  <InputWithType v-bind="{scopeName, field, errors}">
      <select
              class="select-control form-control"
              :id="`${scopeName}-${field}`"
              :name="field"
              v-model="modelValue"
              @input="$emit('update:modelValue', $event.target.value)"
      >

        <option class="options" v-for="option in options" :key="option.value" :value="option.value">{{option.label}}</option>
      </select>
  </InputWithType>
</template>

<script setup>
import { defineEmit } from "@vue/runtime-core"
import InputWithType from "./InputWithType.vue"
  const props = defineProps({modelValue: String, scopeName: String, field: String, options: Array, errors: Object})
  const emit = defineEmit(['update:modelValue'])

</script>
