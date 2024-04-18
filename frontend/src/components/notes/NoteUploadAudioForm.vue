<template>
  <AudioFileInput
    scope-name="note"
    field="uploadAudioFile"
    placeholder="Optional. upload own picture."
    :errors="errors.uploadAudioFileProxy"
    :model-value="modelValue.uploadPictureProxy"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, uploadPictureProxy: $event })
    "
  />
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { NoteAccessoriesDTO } from "@/generated/backend";
import AudioFileInput from "../form/AudioFileInput.vue";

export default defineComponent({
  props: {
    modelValue: {
      type: Object as PropType<NoteAccessoriesDTO>,
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
      formData: { ...this.modelValue, convertToSrt: false },
    };
  },
  emits: ["update:modelValue"],
  components: { AudioFileInput },
});
</script>
