<template>
  <InputWithType v-bind="{ scopeName, field, errorMessage }">
    <output :id="`${scopeName}-${field}`" role="radiogroup" class="daisy:join daisy:flex daisy:flex-wrap">
      <template v-for="option in options" :key="option.value">
        <input
          class="daisy:join-item daisy:hidden"
          type="radio"
          :value="option.value"
          :id="`${scopeName}-${option.value}`"
          :checked="modelValue === option.value"
          @change="selectionChanged"
        />
        <label
          role="button"
          :title="option.title"
          :class="['daisy:btn', 'daisy:btn-outline', 'daisy:join-item', 'daisy:text-nowrap', { 'daisy:bg-primary daisy:border-primary': modelValue === option.value }]"
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
import type { PropType } from "vue"
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
.join {
  flex-wrap: wrap;
}

label {
  font-size: small;
}
</style>
