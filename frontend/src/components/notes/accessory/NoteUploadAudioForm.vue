<template>
  <FileInput
    scope-name="note"
    field="uploadAudioFile"
    accept=".mp3, .m4a, .wav"
    :errors="errors.uploadAudioFile"
    :model-value="modelValue.uploadAudioFile"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, uploadAudioFile: $event })
    "
  />
</template>

<script lang="ts">
import { AudioUploadDTO } from "@/generated/backend"
import { PropType, defineComponent } from "vue"
import FileInput from "../../form/FileInput.vue"

export default defineComponent({
  props: {
    modelValue: {
      type: Object as PropType<AudioUploadDTO>,
      required: true,
    },
    errors: {
      type: Object,
      default() {
        return {}
      },
    },
  },
  data() {
    return {
      formData: { ...this.modelValue },
    }
  },
  emits: ["update:modelValue"],
  components: { FileInput },
})
</script>
