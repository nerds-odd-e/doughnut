<template>
  <div ref="editor"></div>
</template>

<script setup lang="ts">
import { nextTick, ref, onMounted, watch } from "vue"
import Quill, { type QuillOptions } from "quill"
import "quill/dist/quill.bubble.css"
import markdownizer from "./markdownizer"
import { useInterruptingHtmlToMarkdown } from "@/composables/useInterruptingHtmlToMarkdown"

const { modelValue, readonly } = defineProps({
  modelValue: String,
  readonly: Boolean,
})

const emits = defineEmits(["update:modelValue", "blur"])

const localValue = ref(modelValue)
const editor = ref<HTMLElement | null>(null)
const quill = ref<Quill | null>(null)
const { htmlToMarkdown } = useInterruptingHtmlToMarkdown()

const onBlurTextField = () => {
  emits("blur")
}

const updateQuillContent = (content: string | undefined) => {
  if (quill.value) {
    quill.value.root.innerHTML = content ?? ""
  }
}

const options: QuillOptions = {
  modules: {
    toolbar: [
      ["bold", "italic", "underline"],
      [{ header: 1 }, { header: 2 }],
      ["blockquote"],
      [{ list: "ordered" }, { list: "bullet" }],
      ["link"],
    ],
  },
  placeholder: readonly ? "" : "Enter note details here...",
  readOnly: readonly,
  theme: "bubble",
}

onMounted(async () => {
  if (editor.value) {
    quill.value = new Quill(editor.value, options)

    // Set initial content
    updateQuillContent(localValue.value)

    // Wait for next tick to ensure Quill is fully initialized
    await nextTick()

    if (!readonly && quill.value) {
      quill.value.root.addEventListener(
        "paste",
        (event: ClipboardEvent) => {
          if (!event.clipboardData) return

          const originalGetData = event.clipboardData.getData.bind(
            event.clipboardData
          )

          event.clipboardData.getData = (format: string) => {
            if (format === "text/html") {
              const htmlData = originalGetData(format)
              if (htmlData) {
                const markdown = htmlToMarkdown(htmlData)
                return markdownizer.markdownToHtml(markdown)
              }
            }
            return originalGetData(format)
          }
        },
        true
      )
    }

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
