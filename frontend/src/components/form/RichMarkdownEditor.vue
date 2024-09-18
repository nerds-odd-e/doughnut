<template>
  <RichHtmlEditor
    v-bind="{ multipleLine, scopeName, field, title, errors }"
    :model-value="htmlValue"
    :readonly="readonly"
    @update:model-value="htmlValueUpdated($event)"
    @blur="$emit('blur')"
  />
</template>

<script setup lang="ts">
import { computed } from "vue"
import "quill/dist/quill.snow.css"
import RichHtmlEditor from "./RichHtmlEditor.vue"
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

const htmlValue = computed(() => markdownizer.markdownToHtml(modelValue))

const htmlValueUpdated = (htmlValue: string) => {
  const markdownValue = markdownizer.htmlToMarkdown(htmlValue)
  emits("update:modelValue", markdownValue)
}
</script>
