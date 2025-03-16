<template>
  <InputWithType v-bind="{ class: $attrs.class, scopeName, field, errorMessage }">
    <textarea
      :class="`daisy:textarea daisy-textarea-bordered daisy:w-full ${!!errorMessage ? 'daisy:textarea-error' : ''}`"
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
  async () => {
    await nextTick()
    resize()
  }
)

onMounted(async () => {
  await nextTick()
  resize()
})
</script>
