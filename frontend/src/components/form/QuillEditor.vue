<template>
  <div ref="editor"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue"
import Quill, { type QuillOptions } from "quill"
import "quill/dist/quill.bubble.css"

const { modelValue, readonly } = defineProps({
  modelValue: String,
  readonly: Boolean,
})

const emits = defineEmits(["update:modelValue", "blur"])

const localValue = ref(modelValue)
const editor = ref<HTMLElement | null>(null)
const quill = ref<Quill | null>(null)

const onBlurTextField = () => {
  emits("blur")
}

const updateQuillContent = (content: string | undefined) => {
  if (quill.value) {
    // Preserve cursor position when updating content from external changes (e.g., API response)
    let savedRange: { index: number; length: number } | null = null
    let isFocused = false
    try {
      savedRange = quill.value.getSelection(true) // true = preserve even if editor not focused
      isFocused = quill.value.hasFocus()
    } catch {
      // Quill might not be fully initialized, ignore
    }

    quill.value.root.innerHTML = content ?? ""

    // Restore cursor position if the editor was focused
    if (isFocused && savedRange) {
      try {
        // Clamp the range to valid bounds
        const length = quill.value.getLength()
        const start = Math.min(savedRange.index, length - 1)
        const end = Math.min(
          savedRange.index + (savedRange.length || 0),
          length - 1
        )
        quill.value.setSelection({ index: start, length: end - start })
      } catch {
        // Ignore errors when setting selection
      }
    }
  }
}

const options: QuillOptions = {
  modules: {
    toolbar: [
      ["bold", "italic", "underline"],
      [{ header: 1 }, { header: 2 }],
      [{ list: "ordered" }, { list: "bullet" }],
      ["link"],
    ],
  },
  placeholder: readonly ? "" : "Enter note details here...",
  readOnly: readonly,
  theme: "bubble",
}

onMounted(() => {
  if (editor.value) {
    quill.value = new Quill(editor.value, options)

    // Set initial content
    updateQuillContent(localValue.value)

    // Listen for text changes
    quill.value.on("text-change", () => {
      const content = quill.value!.root.innerHTML
      localValue.value = content
      onUpdateContent()
    })

    quill.value.on("selection-change", (range) => {
      if (!range) {
        onBlurTextField()
      }
    })

    // Strangely, Quill does not emit a blur event when the inner editor receives a blur event
    quill.value.root.addEventListener("blur", () => {
      quill.value?.blur()
    })
  }
})

// Watch for changes in modelValue prop
watch(
  () => modelValue,
  (newValue) => {
    if (quill.value) {
      // Only update if the value actually changed
      if (localValue.value !== newValue) {
        localValue.value = newValue
        updateQuillContent(newValue)
      } else {
        // Value is the same but cursor might have been reset - try to preserve it
        try {
          const savedRange = quill.value.getSelection(true)
          if (quill.value.hasFocus() && savedRange) {
            const length = quill.value.getLength()
            const start = Math.min(savedRange.index, length - 1)
            const end = Math.min(
              savedRange.index + (savedRange.length || 0),
              length - 1
            )
            quill.value.setSelection({ index: start, length: end - start })
          }
        } catch {
          // Ignore errors when getting/setting selection
        }
      }
    }
  }
)

const onUpdateContent = () => {
  emits("update:modelValue", localValue.value)
}
</script>

<style lang="sass">
.ql-editor
  padding: 0
  margin-bottom: 15px
  &::before
    left: 0 !important
    right: 0 !important
  p
    margin: inherit !important
.ql-container.ql-bubble
  border: none
  font-size: inherit !important
</style>
