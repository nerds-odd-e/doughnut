<template>
  <QuillEditor
    ref="quillEditor"
    v-model:content="localValue"
    :options="{
      modules: {
            toolbar: false,
            keyboard: {
              bindings: {
                custom: {
                  key: 13,
                  shiftKey: true,
                  handler: onBlurTextField,
                },
              },
            },
          },
          placeholder: readonly ? '' : 'Enter note details here...',
    }"
    :content-type="'html'"
    :read-only="readonly"
    @blur="onBlurTextField"
    @update:content="onUpdateContent"
    @focus="hadFocus = true"
  />
</template>

<script setup lang="ts">
import { QuillEditor } from "@vueup/vue-quill"
import { ref, watch } from "vue"

import "quill/dist/quill.snow.css"

const { modelValue, readonly } = defineProps({
  multipleLine: Boolean,
  modelValue: String,
  scopeName: String,
  field: String,
  title: String,
  errors: Object,
  readonly: Boolean,
})

const emits = defineEmits(["update:modelValue", "blur"])

const onBlurTextField = () => {
  emits("blur")
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
