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

      // Wait for Quill to be fully initialized and ready
      await nextTick()

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

      // Set initial content with more robust approach
      if (localValue.value) {
        // Wait longer to ensure Quill is completely ready
        setTimeout(() => {
          if (quill.value) {
            try {
              // First try to set content via innerHTML directly (safer for initialization)
              quill.value.root.innerHTML = localValue.value || ""
              // Then clear selection to avoid selection errors
              quill.value.setSelection(null)
            } catch (error) {
              // Fallback to setText if innerHTML fails
              try {
                quill.value.setText(localValue.value || "")
              } catch (fallbackError) {
                // Silent fallback - content initialization errors are not critical
              }
            }
          }
        }, 100)
      }
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

      // Use a longer timeout to ensure Quill is stable and ready
      setTimeout(() => {
        if (!quill.value) return

        try {
          // Check if Quill is in a stable state before proceeding
          if (!quill.value.root || !quill.value.root.innerHTML) {
            return
          }

          // For watch updates, use direct innerHTML first (more reliable)
          try {
            if (quill.value.root) {
              quill.value.root.innerHTML = newValue || ""
              // Clear selection after direct content change to avoid errors
              try {
                quill.value.setSelection(null)
              } catch (selectionError) {
                // Ignore selection errors, they're not critical
              }
            }
          } catch (error) {
            // Fallback to setText
            try {
              quill.value.setText(newValue || "")
            } catch (textError) {
              console.error("setText also failed in watch:", textError)
            }
          }
        } catch (error) {
          console.error("Unexpected error in watch:", error)
        }
      }, 50) // Increased timeout for better stability
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
