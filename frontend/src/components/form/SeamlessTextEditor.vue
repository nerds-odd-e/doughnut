<template>
  <div
    ref="editor"
    class="seamless-editor"
    :role="role"
    :contenteditable="!readonly"
    @input="onInput"
    @blur="onBlur"
    @keydown.enter.prevent="onEnter"
  ></div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from "vue"

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

const updateContent = (newValue: string) => {
  if (!editor.value) return

  const currentValue = editor.value.innerText
  const selection = window.getSelection()
  const range = selection?.rangeCount ? selection.getRangeAt(0) : null
  const isFocused = document.activeElement === editor.value

  // Save cursor position relative to the text content
  let savedOffset = 0
  if (
    isFocused &&
    range &&
    editor.value.contains(range.commonAncestorContainer)
  ) {
    // Calculate offset from start of editor
    const preRange = range.cloneRange()
    preRange.selectNodeContents(editor.value)
    preRange.setEnd(range.startContainer, range.startOffset)
    savedOffset = preRange.toString().length
  }

  // Only update if the value actually changed
  if (currentValue !== newValue) {
    // First update innerText to ensure it's set properly for tests and initial render
    editor.value.innerText = newValue

    // Then update or create a text node to handle cursor position
    // This fixes the cursor jumping issue in Safari and Chrome mobile
    // by maintaining a single text node instead of recreating the content
    // which would reset cursor position
    if (editor.value.firstChild) {
      ;(editor.value.firstChild as Text).data = newValue
    } else {
      const textNode = document.createTextNode(newValue)
      editor.value.appendChild(textNode)
    }

    // Restore cursor position if the editor was focused
    if (isFocused && savedOffset > 0 && editor.value.firstChild) {
      const textNode = editor.value.firstChild as Text
      // Clamp offset to valid range
      const clampedOffset = Math.min(savedOffset, newValue.length)
      const newRange = document.createRange()
      newRange.setStart(textNode, clampedOffset)
      newRange.setEnd(textNode, clampedOffset)
      selection?.removeAllRanges()
      selection?.addRange(newRange)
    }
  } else if (isFocused && savedOffset > 0 && editor.value.firstChild) {
    // Value is the same but cursor might have been reset - restore it
    const textNode = editor.value.firstChild as Text
    const clampedOffset = Math.min(savedOffset, newValue.length)
    const newRange = document.createRange()
    newRange.setStart(textNode, clampedOffset)
    newRange.setEnd(textNode, clampedOffset)
    selection?.removeAllRanges()
    selection?.addRange(newRange)
  }
}

// Keep the editor content in sync with external changes
watch(() => props.modelValue, updateContent)

// Initialize content
onMounted(() => {
  if (editor.value) {
    updateContent(props.modelValue)
  }
})
</script>

<style scoped>
.seamless-editor {
  outline: none;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: thin;
  -ms-overflow-style: -ms-autohiding-scrollbar;
}

/* Show webkit scrollbar (Chrome, Safari, newer Edge) */
.seamless-editor::-webkit-scrollbar {
  height: 4px;
}

.seamless-editor::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.seamless-editor::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 2px;
}

.seamless-editor:empty:before {
  content: attr(placeholder);
  color: #888;
}
</style>
