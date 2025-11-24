<template>
  <InputWithType v-bind="{ class: $attrs.class, scopeName, field, errorMessage }">
    <textarea
      :class="`daisy-textarea daisy-textarea-bordered daisy-w-full ${!!errorMessage ? 'daisy-textarea-error' : ''}`"
      :id="`${scopeName}-${field}`"
      :name="field"
      :value="internalValue"
      @input="handleInput"
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
import { nextTick, onMounted, ref, watch } from "vue"
import InputWithType from "./InputWithType.vue"

const props = defineProps({
  modelValue: String,
  scopeName: String,
  field: String,
  placeholder: { type: String, default: null },
  autofocus: { type: Boolean, default: false },
  enterSubmit: { type: Boolean, default: false },
  rows: { type: Number, default: 8 },
  autoExtendUntil: { type: Number, default: null },
  errorMessage: String,
})

const emit = defineEmits(["update:modelValue", "blur", "enterPressed"])
const input = ref<HTMLTextAreaElement | null>(null)
const internalValue = ref(props.modelValue || "")

const focus = () => {
  if (input.value === null) return
  input.value.focus()
}

defineExpose({
  focus,
})

const handleInput = (event: Event) => {
  const newValue = (event.target as HTMLTextAreaElement).value
  internalValue.value = newValue
  emit("update:modelValue", newValue)
}

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

const resize = () => {
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

watch(
  () => props.modelValue,
  async (newValue) => {
    if (input.value && newValue !== undefined && newValue !== null) {
      // Preserve cursor position when updating value from external changes (e.g., API response)
      const currentValue = internalValue.value
      const selectionStart = input.value.selectionStart
      const selectionEnd = input.value.selectionEnd
      const isFocused = document.activeElement === input.value

      // Only update internal value if it actually changed
      if (currentValue !== newValue) {
        internalValue.value = newValue
        // Restore cursor position if the textarea was focused
        if (isFocused && selectionStart !== undefined) {
          // Try to preserve cursor position, clamping to valid range
          const newSelectionStart = Math.min(selectionStart, newValue.length)
          const newSelectionEnd = Math.min(selectionEnd, newValue.length)
          await nextTick()
          input.value.setSelectionRange(newSelectionStart, newSelectionEnd)
        }
      } else if (isFocused && selectionStart !== undefined) {
        // Value is the same - ensure cursor position is preserved
        await nextTick()
        input.value.setSelectionRange(selectionStart, selectionEnd)
      }
    } else {
      internalValue.value = newValue || ""
    }
    await nextTick()
    resize()
  }
)

onMounted(async () => {
  internalValue.value = props.modelValue || ""
  await nextTick()
  resize()
})
</script>
