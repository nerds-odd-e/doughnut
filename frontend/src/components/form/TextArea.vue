<template>
  <InputWithType v-bind="{ class: $attrs.class, scopeName, field, errors: errorMessage }">
    <textarea
      :class="`area-control form-control ${!!errors ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="field"
      :value="modelValue"
      @input="
        emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)
      "
      :placeholder="placeholder"
      :autofocus="autofocus"
      autocomplete="off"
      autocapitalize="off"
      :rows="rows"
      @blur="emit('blur', $event)"
      ref="input"
      @keydown="handleKeydown"
    />
  </InputWithType>
</template>

<script setup lang="ts">
import { nextTick, ref, watch, computed, PropType } from "vue"
import InputWithType from "./InputWithType.vue"

type ErrorRecord = Record<string, string | string[]>

const props = defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  placeholder: { type: String, default: null },
  autofocus: { type: Boolean, default: false },
  enterSubmit: { type: Boolean, default: false },
  rows: { type: Number, default: 8 },
  autoExtendUntil: { type: Number, default: null },
  errors: {
    type: Object as PropType<ErrorRecord | undefined>,
    default: () => ({}),
  },
})

const emit = defineEmits(["update:modelValue", "blur", "enterPressed"])
const input = ref<HTMLTextAreaElement | null>(null)

const focus = () => {
  if (input.value === null) return
  input.value.focus()
}

defineExpose({
  focus,
})

const handleKeydown = (event: KeyboardEvent) => {
  if (
    props.enterSubmit &&
    event.key === "Enter" &&
    !event.shiftKey &&
    !event.isComposing
  ) {
    event.preventDefault() // Prevent newline insertion
    emit("enterPressed")
  }
}

watch(
  () => props.modelValue,
  async () => {
    await nextTick()
    if (input.value && props.autoExtendUntil) {
      const lineHeight = parseInt(
        window.getComputedStyle(input.value).lineHeight,
        10
      )
      const { scrollHeight } = input.value
      const newRows = Math.floor(scrollHeight / lineHeight)
      input.value.rows =
        newRows > props.autoExtendUntil ? props.autoExtendUntil : newRows
    }
  }
)

// Convert errors object to a string message
const errorMessage = computed(() => {
  if (props.errors) {
    return Object.values(props.errors).flat().join(", ")
  }
  return undefined
})
</script>