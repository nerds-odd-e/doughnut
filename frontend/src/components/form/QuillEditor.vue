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
    quill.value.root.innerHTML = localValue.value || ""

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
      quill.value.root.innerHTML = newValue || ""
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
