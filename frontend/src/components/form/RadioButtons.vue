<template>
  <InputWithType v-bind="{ scopeName, field, errorMessage }">
    <output
      :id="`${scopeName}-${field}`"
      role="radiogroup"
      :class="radiogroupClassList"
    >
      <template v-for="option in options" :key="option.value">
        <input
          class="daisy-join-item daisy-hidden"
          type="radio"
          :value="option.value"
          :id="`${scopeName}-${option.value}`"
          :checked="modelValue === option.value"
          @change="selectionChanged"
        />
        <label
          role="button"
          :title="option.title"
          :class="labelClassList(option.value)"
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
import { computed } from "vue"
import type { PropType } from "vue"
import InputWithType from "./InputWithType.vue"

const props = defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  options: Array as PropType<
    { value: string; label: string; title?: string }[]
  >,
  errorMessage: String,
  /** `join`: segmented control (default). `chips`: spaced pills that wrap cleanly. */
  variant: {
    type: String as PropType<"join" | "chips">,
    default: "join",
  },
  /** Extra classes on the radiogroup `output` (e.g. max height + scroll). */
  radiogroupClass: { type: String, default: "" },
})

const radiogroupClassList = computed(() => {
  const base =
    props.variant === "chips"
      ? "radio-buttons-chips daisy-flex daisy-flex-wrap daisy-gap-2 daisy-content-start"
      : "radio-buttons-join daisy-join daisy-flex daisy-flex-wrap"
  return [base, props.radiogroupClass].filter(Boolean)
})

function labelClassList(optionValue: string) {
  const selected = props.modelValue === optionValue
  const classes: (string | Record<string, boolean>)[] = [
    "daisy-btn",
    "daisy-btn-outline",
    "daisy-text-nowrap",
    props.variant === "chips"
      ? "daisy-btn-sm daisy-inline-flex daisy-items-center daisy-gap-1 daisy-rounded-lg daisy-normal-case"
      : "daisy-join-item",
    { "daisy-bg-primary daisy-border-primary": selected },
  ]
  return classes
}

const emit = defineEmits(["update:modelValue"])

const selectionChanged = (event: Event) => {
  emit("update:modelValue", (event.target as HTMLInputElement).value)
}
</script>

<style scoped>
label {
  font-size: small;
}

/* Join variant: restore corners when the group wraps (join borders otherwise look broken). */
.radio-buttons-join label:first-of-type {
  border-top-left-radius: 0.5rem !important;
  border-bottom-left-radius: 0.5rem !important;
}

.radio-buttons-join label:last-of-type {
  border-top-right-radius: 0.5rem !important;
  border-bottom-right-radius: 0.5rem !important;
}
</style>
