<template>
  <AudioFileInput
    scope-name="note"
    field="uploadAudioFile"
    :errors="errors.uploadAudioFile"
    :model-value="modelValue.uploadAudioFile"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, uploadAudioFile: $event })
    "
  />
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { AudioUploadDTO } from "@/generated/backend";
import AudioFileInput from "../form/AudioFileInput.vue";

export default defineComponent({
  props: {
    modelValue: {
      type: Object as PropType<AudioUploadDTO>,
      required: true,
    },
    errors: {
      type: Object,
      default() {
        return {};
      },
    },
  },
  data() {
    return {
      formData: { ...this.modelValue },
    };
  },
  emits: ["update:modelValue"],
  components: { AudioFileInput },
});
</script>
