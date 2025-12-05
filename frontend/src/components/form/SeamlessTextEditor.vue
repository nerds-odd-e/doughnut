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
  const plainText = event.clipboardData?.getData("text/plain") || ""
  if (!editor.value) {
    return
  }

  // Get selection BEFORE preventing default, as preventDefault might affect it
  const selection = window.getSelection()
  const currentText = editor.value.innerText || ""
  let start = currentText.length
  let end = currentText.length

  // Calculate selection range within the editor's text
  // The component maintains a single text node, so we can use range offsets directly
  if (selection && selection.rangeCount > 0) {
    try {
      const range = selection.getRangeAt(0)
      const startContainer = range.startContainer
      const endContainer = range.endContainer

      // Since the component only has one text node, if startContainer is a text node,
      // it's almost certainly our text node. Use a simple check.
      const textNode = editor.value.firstChild
      if (startContainer.nodeType === 3) {
        // It's a text node - if editor has a text node child, assume it's ours
        // This works because we maintain a single text node structure
        if (textNode && textNode.nodeType === 3) {
          start = range.startOffset
          if (endContainer.nodeType === 3) {
            end = range.endOffset
          } else {
            end = range.startOffset
          }
        } else if (startContainer.parentNode === editor.value) {
          // Fallback: check if parent is editor
          start = range.startOffset
          if (endContainer.nodeType === 3 && endContainer.parentNode === editor.value) {
            end = range.endOffset
          } else {
            end = range.startOffset
          }
        }
      } else if (startContainer === editor.value) {
        // Range spans entire editor
        start = 0
        end = currentText.length
      }
    } catch (e) {
      // Range might be invalid, fall back to appending at end
    }
  }

  // Now prevent default after we've captured the selection
  event.preventDefault()

  // Build new text: before selection + pasted text + after selection
  const newText =
    currentText.slice(0, start) + plainText + currentText.slice(end)

  // Update content and model value
  updateContent(newText)
  emits("update:modelValue", newText)

  // Set cursor position after pasted text
  const textNode = editor.value.firstChild as Text | null
  if (textNode) {
    const newCursorPos = start + plainText.length
    const range = document.createRange()
    range.setStart(textNode, Math.min(newCursorPos, textNode.length))
    range.collapse(true)
    const sel = window.getSelection()
    if (sel) {
      sel.removeAllRanges()
      sel.addRange(range)
    }
  }
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
