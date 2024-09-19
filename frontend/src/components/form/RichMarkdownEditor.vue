<template>
  <QuillEditor
    v-bind="{ multipleLine, scopeName, field, title, errors }"
    :model-value="htmlValue"
    :readonly="readonly"
    @update:model-value="htmlValueUpdated"
    @blur="$emit('blur')"
  />
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
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

const emits = defineEmits(["update:modelValue", "blur"])

const htmlValue = ref(markdownizer.markdownToHtml(modelValue))

let currentIntervalMarkdown: string | undefined = undefined
let currentIntervalHtml: string | undefined = undefined

watch(
  () => modelValue,
  (newValue) => {
    if (newValue === currentIntervalMarkdown) {
      htmlValue.value = currentIntervalHtml!
      return
    }
    htmlValue.value = markdownizer.markdownToHtml(newValue)
  }
)

const htmlValueUpdated = (newHtmlValue: string) => {
  const markdownValue = markdownizer.htmlToMarkdown(newHtmlValue)
  currentIntervalMarkdown = markdownValue
  currentIntervalHtml = newHtmlValue
  if (markdownValue === modelValue) return
  emits("update:modelValue", markdownValue)
}
</script>
