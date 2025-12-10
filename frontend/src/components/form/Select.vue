<template>
  <InputWithType v-bind="{ scopeName, field, errorMessage }">
    <select
      :class="`daisy-select daisy-select-bordered ${!!errorMessage ? 'daisy-select-error' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="scopeName"
      :value="modelValue"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <option
        class="options"
        v-for="option in options"
        :key="option"
        :value="option"
      >
        {{ option }}
      </option>
    </select>
  </InputWithType>
</template>

<script lang="ts" setup>
import type { PropType } from "vue"
import InputWithType from "./InputWithType.vue"

const props = defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  options: Array as PropType<string[]>,
  errorMessage: String,
})

const emit = defineEmits(["update:modelValue"])
</script>
