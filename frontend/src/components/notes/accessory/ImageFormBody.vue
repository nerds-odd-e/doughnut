<template>
  <ImageInput
    scope-name="note"
    field="uploadImage"
    placeholder="Optional. upload own image."
    :errors="errors.uploadImage"
    :model-value="modelValue.uploadImage"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, uploadImage: $event })
    "
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
    :errors="errors.useParentImage"
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
