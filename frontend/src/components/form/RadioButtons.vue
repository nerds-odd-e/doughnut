<template>
  <InputWithType v-bind="{ scopeName, field, errorMessage }">
    <output :id="`${scopeName}-${field}`" role="radiogroup" class="btn-group">
      <template v-for="option in options" :key="option.value">
        <input
          class="btn-check"
          type="radio"
          :value="option.value"
          :id="`${scopeName}-${option.value}`"
          :checked="modelValue === option.value"
          @change="selectionChanged"
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
      </template>
    </output>
  </InputWithType>
</template>

<script setup lang="ts">
import { PropType } from "vue"
import InputWithType from "./InputWithType.vue"

defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  options: Array as PropType<
    { value: string; label: string; title?: string }[]
  >,
  errorMessage: String,
})

const emit = defineEmits(["update:modelValue"])

const selectionChanged = (event: Event) => {
  emit("update:modelValue", (event.target as HTMLInputElement).value)
}
</script>

<style scoped>
.btn-group {
  flex-wrap: wrap;
}

label {
  font-size: small;
}
</style>
