<template>
  <FileInput
    scope-name="note"
    field="uploadImage"
    placeholder="Optional. upload own image."
    :errors="getErrorObject('uploadImage')"
    :model-value="uploadImageFileName"
    @update:model-value="handleFileUpload"
  />
  <TextInput
    scope-name="note"
    field="imageUrl"
    placeholder="Full url of existing image."
    :errors="errors.imageUrl"
    :model-value="modelValue.imageUrl"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, imageUrl: $event })
    "
  />
  <CheckInput
    scope-name="note"
    field="useParentImage"
    :model-value="modelValue.useParentImage"
    :errors="getErrorObject('useParentImage')"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, useParentImage: $event })
    "
  />
  <TextInput
    scope-name="note"
    field="imageMask"
    :model-value="modelValue.imageMask"
    :errors="errors.imageMask"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, imageMask: $event })
    "
  />
</template>

<script lang="ts">
import { NoteAccessoriesDTO } from "@/generated/backend"
import { PropType, defineComponent, computed } from "vue"
import CheckInput from "../../form/CheckInput.vue"
import FileInput from "../../form/FileInput.vue"
import TextInput from "../../form/TextInput.vue"

export default defineComponent({
  props: {
    modelValue: {
      type: Object as PropType<NoteAccessoriesDTO>,
      required: true,
    },
    errors: {
      type: Object as PropType<Record<string, string | undefined>>,
      default: () => ({}),
    },
  },
  emits: ["update:modelValue"],
  components: { TextInput, CheckInput, FileInput },
  setup(props, { emit }) {
    const uploadImageFileName = computed(() => {
      const uploadImage = props.modelValue.uploadImage
      if (uploadImage instanceof File) {
        return uploadImage.name
      } else if (uploadImage instanceof Blob) {
        return "Unnamed file"
      }
      return undefined
    })

    const handleFileUpload = (file: File | null) => {
      emit("update:modelValue", {
        ...props.modelValue,
        uploadImage: file || undefined,
      })
    }

    const getErrorObject = (field: string) => {
      const error = props.errors[field]
      return error ? { [field]: error } : undefined
    }

    return {
      uploadImageFileName,
      handleFileUpload,
      getErrorObject,
    }
  },
})
</script>