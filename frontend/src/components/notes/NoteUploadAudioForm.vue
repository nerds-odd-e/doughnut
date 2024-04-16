<template>
  <AudioFileInput
    scope-name="note"
    field="uploadAudioFile"
    placeholder="Optional. upload own picture."
    :errors="errors.uploadAudioFileProxy"
    :model-value="modelValue.uploadAudioFileProxy"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, uploadPictureProxy: $event })
    "
  />
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { NoteAccessories } from "@/generated/backend";
import AudioFileInput from "../form/AudioFileInput.vue";

export default defineComponent({
  props: {
    modelValue: {
      type: Object as PropType<NoteAccessories>,
      required: true,
    },
    errors: {
      type: Object,
      default() {
        return {};
      },
    },
  },
  emits: ["update:modelValue"],
  components: { AudioFileInput },
});
</script>
