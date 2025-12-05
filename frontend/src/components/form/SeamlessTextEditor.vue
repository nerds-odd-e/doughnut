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
  if (props.readonly) return

  event.preventDefault()
  const plainText = event.clipboardData?.getData("text/plain") || ""

  if (!editor.value) return

  const selection = window.getSelection()
  if (!selection || selection.rangeCount === 0) {
    // If no selection, just append at the end
    const textNode = document.createTextNode(plainText)
    editor.value.appendChild(textNode)
    selection?.removeAllRanges()
    const range = document.createRange()
    range.selectNodeContents(editor.value)
    range.collapse(false)
    selection?.addRange(range)
  } else {
    const range = selection.getRangeAt(0)
    range.deleteContents()
    const textNode = document.createTextNode(plainText)
    range.insertNode(textNode)
    // Move cursor to end of inserted text
    range.setStartAfter(textNode)
    range.collapse(true)
    selection.removeAllRanges()
    selection.addRange(range)
  }

  // Trigger input event to update modelValue
  editor.value.dispatchEvent(new Event("input", { bubbles: true }))
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

defineExpose({
  onPaste,
  editor,
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
