<template>
  <QuillEditor
    v-model="localValue"
    :readonly="readonly"
    @blur="$emit('blur')"
  />
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from "vue"
import QuillEditor from "./QuillEditor.vue"
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

const localValue = ref(modelValue)

watch(
  () => modelValue,
  () => {
    localValue.value = modelValue
  }
)

watch(
  () => localValue.value,
  () => {
    if (localValue.value === modelValue) {
      return
    }
    emits("update:modelValue", localValue.value)
  }
)
</script>
