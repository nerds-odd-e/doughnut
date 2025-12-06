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
import { ref, watch, onMounted, nextTick } from "vue"

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

const onPaste = (event: ClipboardEvent) => {
  if (props.readonly || !editor.value) {
    return
  }

  event.preventDefault()

  const plainText = event.clipboardData?.getData("text/plain") || ""

  if (!plainText) {
    return
  }

  const currentText = editor.value.innerText || ""
  const selection = window.getSelection()
  let newText = ""
  let cursorPos = 0

  if (selection && selection.rangeCount > 0) {
    try {
      const range = selection.getRangeAt(0)
      const startContainer = range.startContainer
      const textNode = editor.value.firstChild as Text | null

      // Try to use selection if it's in the editor's text node
      // Check both direct reference and parent node to handle different scenarios
      const isInTextNode =
        (textNode && startContainer === textNode) ||
        (startContainer.nodeType === Node.TEXT_NODE &&
          startContainer.parentNode === editor.value)

      if (isInTextNode && textNode) {
        const offset = range.startOffset
        const endOffset = range.endOffset
        const beforeText = currentText.substring(0, offset)
        const afterText = currentText.substring(endOffset)
        newText = beforeText + plainText + afterText
        cursorPos = beforeText.length + plainText.length
      } else if (
        editor.value.contains(startContainer) ||
        editor.value === startContainer
      ) {
        // Selection is in editor - use range operations
        const beforeRange = document.createRange()
        beforeRange.selectNodeContents(editor.value)
        beforeRange.setEnd(range.startContainer, range.startOffset)
        const beforeText = beforeRange.toString()

        const afterRange = document.createRange()
        afterRange.selectNodeContents(editor.value)
        afterRange.setStart(range.endContainer, range.endOffset)
        const afterText = afterRange.toString()

        newText = beforeText + plainText + afterText
        cursorPos = beforeText.length + plainText.length
      } else {
        // Selection is outside editor, append to end
        newText = currentText + plainText
        cursorPos = newText.length
      }
    } catch (e) {
      // If anything fails, fall back to appending
      newText = currentText + plainText
      cursorPos = newText.length
    }
  } else {
    // No selection, append to end
    newText = currentText + plainText
    cursorPos = newText.length
  }

  // Update content and model value
  updateContent(newText)
  emits("update:modelValue", newText)

  // Set cursor position after pasted text
  nextTick(() => {
    if (editor.value) {
      const textNode = editor.value.firstChild as Text | null
      if (textNode) {
        const range = document.createRange()
        const pos = Math.min(cursorPos, textNode.length)
        range.setStart(textNode, pos)
        range.setEnd(textNode, pos)
        selection?.removeAllRanges()
        selection?.addRange(range)
      }
    }
  })
}

const updateContent = (newValue: string) => {
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
