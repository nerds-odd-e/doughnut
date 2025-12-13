<template>
  <QuillEditor
    v-bind="{ multipleLine, scopeName, field, title, errors }"
    :model-value="htmlValue"
    :readonly="readonly"
    @update:model-value="htmlValueUpdated"
    @blur="$emit('blur')"
    @paste-complete="onPasteComplete"
  />
</template>

<script setup lang="ts">
import { computed } from "vue"
import QuillEditor from "./QuillEditor.vue"
import markdownizer from "./markdownizer"

const { modelValue } = defineProps({
  multipleLine: Boolean,
  modelValue: String,
  scopeName: String,
  field: String,
  title: String,
  errors: Object,
  readonly: Boolean,
})

const emits = defineEmits<{
  (e: "update:modelValue", value: string): void
  (e: "blur"): void
  (e: "pasteComplete", value: string): void
}>()

let currentIntervalMarkdown: string | undefined = undefined
let currentIntervalHtml: string | undefined = undefined

const htmlValue = computed(() => {
  if (modelValue === currentIntervalMarkdown) {
    return currentIntervalHtml!
  }
  return markdownizer.markdownToHtml(modelValue)
})

const htmlValueUpdated = (newHtmlValue: string) => {
  const markdownValue = markdownizer.htmlToMarkdown(newHtmlValue)
  currentIntervalMarkdown = markdownValue
  currentIntervalHtml = newHtmlValue
  if (markdownValue === modelValue) return
  emits("update:modelValue", markdownValue)
}

const onPasteComplete = (html: string) => {
  // Convert current HTML content to markdown and emit
  const markdown = markdownizer.htmlToMarkdown(html)
  emits("pasteComplete", markdown)
}
</script>
