<template>
  <PathNameEditor
    :model-value="localValue"
    :error-message="nameError"
    hide-label
    editor-role="heading"
    :editor-data-test="editorDataTest"
    @update:model-value="proposeName"
    @blur="flushName"
  >
    <template #title="{ bindings, editor }">
      <h1 class="text-xl font-semibold text-base-content">
        <component :is="editor" v-bind="bindings" />
      </h1>
    </template>
  </PathNameEditor>
</template>

<script setup lang="ts">
import { ref } from "vue"
import PathNameEditor from "@/components/notes/core/PathNameEditor.vue"
import { useDebouncedTextAutosave } from "@/composables/useDebouncedTextAutosave"
import { toOpenApiError } from "@/managedApi/openApiError"

const props = defineProps<{
  name: string
  editorDataTest: string
  emptyErrorMessage: string
  saveErrorMessage: string
  persistName: (name: string) => Promise<void>
}>()

const nameError = ref<string | undefined>(undefined)

const {
  localValue,
  propose,
  flush: flushAutosave,
  cancel,
} = useDebouncedTextAutosave({
  externalValue: () => props.name,
  persist: props.persistName,
  normalize: (value) => value.trim(),
  onError: (error) => {
    const apiError = toOpenApiError(error)
    nameError.value =
      apiError.errors?.name ?? apiError.message ?? props.saveErrorMessage
  },
})

const proposeName = (value: string) => {
  cancel()
  nameError.value = undefined
  propose(value)
  if (value.trim() === "") {
    cancel()
    nameError.value = props.emptyErrorMessage
  }
}

const flushName = () => {
  if (localValue.value.trim() === "") {
    cancel()
    return
  }
  flushAutosave()
}
</script>
