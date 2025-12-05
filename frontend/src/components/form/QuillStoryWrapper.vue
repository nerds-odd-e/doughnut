<template>
  <div ref="editor" class="quill-story-wrapper"></div>
</template>

<script setup lang="ts">
import { onMounted, ref, nextTick } from "vue"
import Quill, { type QuillOptions } from "quill"
import "quill/dist/quill.bubble.css"

const props = defineProps<{
  initialContent: string
  options?: QuillOptions
}>()

const editor = ref<HTMLElement | null>(null)
const quill = ref<Quill | null>(null)

onMounted(async () => {
  if (editor.value) {
    const defaultOptions: QuillOptions = {
      modules: {
        toolbar: [
          ["bold", "italic", "underline"],
          [{ header: 1 }, { header: 2 }],
          ["blockquote"],
          [{ list: "ordered" }, { list: "bullet" }],
          ["link"],
        ],
      },
      placeholder: "Enter text here...",
      theme: "bubble",
      ...props.options,
    }

    quill.value = new Quill(editor.value, defaultOptions)

    // Wait for next tick to ensure Quill is fully initialized
    await nextTick()

    // Set initial content
    if (quill.value && props.initialContent) {
      quill.value.root.innerHTML = props.initialContent
    }
  }
})
</script>

<style scoped>
.quill-story-wrapper {
  min-height: 200px;
}
</style>

