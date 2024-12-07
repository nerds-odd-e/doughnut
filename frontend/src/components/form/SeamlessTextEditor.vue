<template>
  <div
    ref="editor"
    class="seamless-editor"
    :role="role"
    :contenteditable="!readonly"
    @input="onInput"
    @blur="onBlur"
    @keydown.enter.prevent="onEnter"
    v-text="modelValue"
  ></div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"

const props = defineProps({
  modelValue: { type: String, required: true },
  readonly: { type: Boolean, default: false },
  role: { type: String, required: false },
})

const emits = defineEmits(["update:modelValue", "blur"])
const editor = ref<HTMLElement | null>(null)

const onInput = (event: Event) => {
  const target = event.target as HTMLElement
  // Strip any HTML that might have been pasted
  const plainText = target.innerText
  emits("update:modelValue", plainText)
}

const onBlur = () => {
  emits("blur")
}

const onEnter = (event: KeyboardEvent) => {
  event.target?.dispatchEvent(new Event("blur"))
}

// Keep the editor content in sync with external changes
watch(
  () => props.modelValue,
  (newValue) => {
    if (editor.value && editor.value.innerText !== newValue) {
      editor.value.innerText = newValue
    }
  }
)
</script>

<style scoped>
.seamless-editor {
  outline: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.seamless-editor:empty:before {
  content: attr(placeholder);
  color: #888;
}
</style>
