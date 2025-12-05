<template>
  <div
    ref="editor"
    class="seamless-editor"
    :role="role"
    :contenteditable="!readonly"
    @input="onInput"
    @blur="onBlur"
    @keydown.enter.prevent="onEnter"
    @paste="onPaste"
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
const isHandlingPaste = ref(false)

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

const onPaste = (event: ClipboardEvent) => {
  event.preventDefault()
  const clipboardData = event.clipboardData
  if (!clipboardData || !editor.value) {
    return
  }

  isHandlingPaste.value = true

  // Extract plain text from clipboard
  const plainText = clipboardData.getData("text/plain")

  // Get current selection
  const selection = window.getSelection()
  if (!selection) {
    isHandlingPaste.value = false
    return
  }

  // Get current text
  const currentText = editor.value.innerText || ""

  // Get or create a range and calculate positions
  let range: Range
  let startPos: number
  let endPos: number

  if (selection.rangeCount > 0) {
    range = selection.getRangeAt(0)
    // Always calculate positions using range measurement for reliability
    const measureRange = document.createRange()
    measureRange.selectNodeContents(editor.value)
    measureRange.setEnd(range.startContainer, range.startOffset)
    startPos = measureRange.toString().length
    measureRange.setEnd(range.endContainer, range.endOffset)
    endPos = measureRange.toString().length
  } else {
    // No selection, paste at end
    startPos = currentText.length
    endPos = currentText.length
    range = document.createRange()
    if (editor.value.firstChild) {
      const textNode = editor.value.firstChild as Text
      range.setStart(textNode, textNode.textContent?.length || 0)
      range.collapse(true)
    } else {
      range.selectNodeContents(editor.value)
      range.collapse(false)
    }
  }

  // Build new text: before selection + pasted text + after selection
  const newText =
    currentText.slice(0, startPos) + plainText + currentText.slice(endPos)

  // Update the content while maintaining single text node structure
  if (editor.value.firstChild) {
    ;(editor.value.firstChild as Text).data = newText
  } else {
    const textNode = document.createTextNode(newText)
    editor.value.appendChild(textNode)
  }

  // Set cursor position after inserted text
  const newCursorPos = startPos + plainText.length
  if (editor.value.firstChild) {
    const textNode = editor.value.firstChild as Text
    const newRange = document.createRange()
    newRange.setStart(
      textNode,
      Math.min(newCursorPos, textNode.textContent?.length || 0)
    )
    newRange.collapse(true)
    selection.removeAllRanges()
    selection.addRange(newRange)
  }

  // Emit the update directly
  emits("update:modelValue", newText)

  isHandlingPaste.value = false
}

const updateContent = (newValue: string) => {
  // Skip update if we're handling paste to avoid interference
  if (isHandlingPaste.value) {
    return
  }
  if (editor.value && editor.value.innerText !== newValue) {
    editor.value.innerText = newValue
    // Maintain single text node to prevent cursor jumping in Safari/Chrome mobile
    if (editor.value.firstChild) {
      ;(editor.value.firstChild as Text).data = newValue
    } else {
      const textNode = document.createTextNode(newValue)
      editor.value.appendChild(textNode)
    }
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
