<template>
  <ImageInput
    scope-name="note"
    field="uploadPicture"
    placeholder="Optional. upload own picture."
    :errors="errors.uploadPicture"
    :model-value="modelValue.uploadPicture"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, uploadPicture: $event })
    "
  />
  <TextInput
    scope-name="note"
    field="imageUrl"
    placeholder="Full url of existing picture."
    :errors="errors.imageUrl"
    :model-value="modelValue.imageUrl"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, imageUrl: $event })
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
    field="imageMask"
    :model-value="modelValue.imageMask"
    :errors="errors.imageMask"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, imageMask: $event })
    "
  />
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import { NoteAccessoriesDTO } from "@/generated/backend";
import TextInput from "../../form/TextInput.vue";
import CheckInput from "../../form/CheckInput.vue";
import ImageInput from "../../form/ImageInput.vue";

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
  components: { TextInput, CheckInput, ImageInput },
});
</script>
