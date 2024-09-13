<template>
  <InputWithType v-bind="{ scopeName, field, errors: errorMessage }">
    <select
      :class="`select-control form-control ${!!errors ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="scopeName"
      v-model="localValue"
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
import { PropType, ref, watch, computed } from "vue"
import InputWithType from "./InputWithType.vue"

type ErrorRecord = Record<string, string | string[]>

const props = defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  options: Array as PropType<string[]>,
  errors: {
    type: Object as PropType<ErrorRecord | undefined>,
    default: () => ({}),
  },
})

const emit = defineEmits(["update:modelValue"])
const localValue = ref(props.modelValue)

watch(localValue, (newValue) => {
  emit("update:modelValue", newValue)
})

// Convert errors object to a string message
const errorMessage = computed(() => {
  if (props.errors) {
    return Object.values(props.errors).flat().join(", ")
  }
  return undefined
})
</script>