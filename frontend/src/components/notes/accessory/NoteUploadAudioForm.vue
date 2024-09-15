<template>
  <FileInput
    scope-name="note"
    field="uploadAudioFile"
    accept=".mp3, .m4a, .wav"
    :error-message="errors.uploadAudioFile"
    :model-value="uploadAudioFileName"
    @update:model-value="handleFileUpload"
  />
</template>

<script lang="ts">
import { AudioUploadDTO } from "@/generated/backend"
import { PropType, defineComponent, computed } from "vue"
import FileInput from "../../form/FileInput.vue"

export default defineComponent({
  props: {
    modelValue: {
      type: Object as PropType<AudioUploadDTO>,
      required: true,
    },
    errors: {
      type: Object as PropType<Record<string, string | undefined>>,
      default: () => ({}),
    },
  },
  emits: ["update:modelValue"],
  components: { FileInput },
  setup(props, { emit }) {
    const uploadAudioFileName = computed(() => {
      const uploadAudioFile = props.modelValue.uploadAudioFile
      if (uploadAudioFile instanceof File) {
        return uploadAudioFile.name
      } else if (uploadAudioFile instanceof Blob) {
        return "Unnamed audio file"
      }
      return undefined
    })

    const handleFileUpload = (file: File | null) => {
      emit("update:modelValue", {
        ...props.modelValue,
        uploadAudioFile: file || undefined,
      })
    }

    return {
      uploadAudioFileName,
      handleFileUpload,
    }
  },
})
</script>
