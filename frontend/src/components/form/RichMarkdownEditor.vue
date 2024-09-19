<template>
  <QuillEditor
    v-bind="{ multipleLine, scopeName, field, title, errors }"
    :model-value="htmlValue"
    :model-refresher="modelRefresher"
    :readonly="readonly"
    @update:model-value="htmlValueUpdated"
    @blur="$emit('blur')"
  />
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import QuillEditor from "./QuillEditor.vue"
import markdownizer from "./markdownizer"

const props = defineProps({
  multipleLine: Boolean,
  modelValue: String,
  scopeName: String,
  field: String,
  title: String,
  errors: Object,
  readonly: Boolean,
})

const emits = defineEmits(["update:modelValue", "blur"])

// This is a hack to force QuillEditor to refresh when the modelValue changes
const modelRefresher = ref(0)

const htmlValue = ref(markdownizer.markdownToHtml(props.modelValue))
let internalModelValue: string | undefined = undefined

watch(
  () => props.modelValue,
  (newModelValue) => {
    if (internalModelValue !== newModelValue) {
      internalModelValue = newModelValue
      htmlValue.value = markdownizer.markdownToHtml(newModelValue)
      modelRefresher.value++
    }
  }
)

const htmlValueUpdated = (newHtmlValue: string) => {
  const markdownValue = markdownizer.htmlToMarkdown(newHtmlValue)
  internalModelValue = markdownValue
  emits("update:modelValue", markdownValue)
}
</script>
