<template>
  <MediaInput
    scope-name="note"
    field="uploadPicture"
    placeholder="Optional. upload own picture."
    accept="*"
    title="Upload picture file"
    :errors="errors.uploadPictureProxy"
    :model-value="modelValue.uploadPictureProxy"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, uploadPictureProxy: $event })
    "
  />
  <TextInput
    scope-name="note"
    field="pictureUrl"
    placeholder="Full url of existing picture."
    :errors="errors.pictureUrl"
    :model-value="modelValue.pictureUrl"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, pictureUrl: $event })
    "
  />
  <CheckInput
    scope-name="note"
    field="useParentPicture"
    :model-value="modelValue.useParentPicture"
    :errors="errors.useParentPicture"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, useParentPicture: $event })
    "
  />
  <TextInput
    scope-name="note"
    field="pictureMask"
    :model-value="modelValue.pictureMask"
    :errors="errors.pictureMask"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, pictureMask: $event })
    "
  />
  <TextInput
    scope-name="note"
    field="url"
    :model-value="modelValue.url"
    :errors="errors.url"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, url: $event })
    "
  />

  <MediaInput
    scope-name="note"
    field="attachAudio"
    placeholder="Optional. upload own audio."
    accept=".mp3, .m4a"
    title="Attach audio file"
    :errors="errors.uploadPictureProxy"
    :model-value="modelValue.uploadPictureProxy"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, uploadPictureProxy: $event })
    "
  />
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { NoteAccessoriesDTO } from "@/generated/backend";
import TextInput from "../form/TextInput.vue";
import CheckInput from "../form/CheckInput.vue";
import MediaInput from "../form/MediaInput.vue";

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
  emits: ["update:modelValue"],
  components: { TextInput, CheckInput, MediaInput },
});
</script>
