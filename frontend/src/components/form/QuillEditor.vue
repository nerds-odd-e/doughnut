<template>
  <div ref="editor"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from "vue"
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
    quill.value.root.innerHTML = content ?? ""
  }
}

const modules = readonly
  ? { toolbar: false }
  : {
      toolbar: [
        ["bold", "italic", "underline"],
        [{ header: 1 }, { header: 2 }],
        [{ list: "ordered" }, { list: "bullet" }],
        ["link"],
      ],
    }

const options: QuillOptions = {
  modules,
  placeholder: readonly ? "" : "Enter note details here...",
  readOnly: readonly,
  theme: "bubble",
}

onMounted(async () => {
  // Wait for DOM to be fully rendered
  await nextTick()

  if (editor.value) {
    try {
      quill.value = new Quill(editor.value, options)

      // Set initial content
      updateQuillContent(localValue.value)

      // Set up event listeners first before any content operations
      quill.value.on("text-change", (_delta, _oldDelta, source) => {
        // Only propagate user-initiated edits to avoid feedback loops
        if (source !== "user") return
        try {
          const content = quill.value!.root.innerHTML
          localValue.value = content
          onUpdateContent()
        } catch (error) {
          console.error("Error handling text change:", error)
        }
      })

      quill.value.on("selection-change", (range) => {
        try {
          if (!range) {
            onBlurTextField()
          }
        } catch (error) {
          console.error("Error handling selection change:", error)
        }
      })

      // Strangely, Quill does not emit a blur event when the inner editor receives a blur event
      quill.value.root.addEventListener("blur", () => {
        try {
          quill.value?.blur()
        } catch (error) {
          console.error("Error handling blur:", error)
        }
      })
    } catch (error) {
      console.error("Error initializing Quill editor:", error)
    }
  }
})

// Watch for changes in modelValue prop
watch(
  () => modelValue,
  (newValue) => {
    if (quill.value && localValue.value !== newValue) {
      localValue.value = newValue
      updateQuillContent(newValue)
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
