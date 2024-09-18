<template>
  <QuillEditor
    ref="quillEditor"
    v-model:content="localValue"
    :options="options"
    :content-type="'html'"
    :read-only="readonly"
    @blur="onBlurTextField"
    @update:content="onUpdateContent"
    @focus="hadFocus = true"
  />
</template>

<script setup lang="ts">
import { QuillEditor } from "@vueup/vue-quill"
import { onBeforeUnmount, onMounted, ref, watch } from "vue"

import "quill/dist/quill.snow.css"
import type { QuillOptions } from "quill"

const { modelValue, readonly } = defineProps({
  multipleLine: Boolean,
  modelValue: String,
  scopeName: String,
  field: String,
  title: String,
  errors: Object,
  readonly: Boolean,
})

const quillEditor = ref<null | HTMLElement>(null)

const onBlurTextField = () => {
  emits("blur")
}

onMounted(() => {
  quillEditor.value = document.querySelector(".ql-editor")
  if (!quillEditor.value) {
    return
  }
  quillEditor.value.addEventListener("blur", () => {
    onBlurTextField()
  })
})

const emits = defineEmits(["update:modelValue", "blur"])

const options: QuillOptions = {
  modules: {
    toolbar: false,
  },
  placeholder: readonly ? "" : "Enter note details here...",
}

const localValue = ref(modelValue)
const hadFocus = ref(false)

watch(
  () => modelValue,
  () => {
    localValue.value = modelValue
  }
)

const onUpdateContent = () => {
  if (localValue.value === modelValue) {
    return
  }
  emits("update:modelValue", localValue.value)
}

onBeforeUnmount(() => {
  onBlurTextField()
})
</script>

<style lang="sass">
.ql-editor
  padding: 0
  margin-bottom: 15px
  &::before
    left: 0 !important
    right:0 !important
  li
    list-style-type: inherit
.ql-container.ql-snow
  border: none
  font-size: inherit !important
</style>
