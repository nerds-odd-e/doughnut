<template>
  <AudioFileInput
    scope-name="note"
    field="uploadAudioFile"
    :errors="errors.file"
    :model-value="modelValue.file"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, file: $event })
    "
  />
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import AudioFileInput from "../form/AudioFileInput.vue";

export default defineComponent({
  props: {
    modelValue: {
      type: Object as PropType<{ file: Blob }>,
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
