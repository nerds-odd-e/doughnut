<template>
  <ImageInput
    scope-name="note"
    field="uploadPicture"
    placeholder="Optional. upload own picture."
    :errors="errors.uploadPictureProxy"
    v-model="modelValue.uploadPictureProxy"
  />
  <TextInput
    scope-name="note"
    field="pictureUrl"
    placeholder="Full url of existing picture."
    :errors="errors.pictureUrl"
    v-model="modelValue.pictureUrl"
  />
  <CheckInput
    scope-name="note"
    field="useParentPicture"
    v-model="modelValue.useParentPicture"
    :errors="errors.useParentPicture"
  />
  <TextInput
    scope-name="note"
    field="pictureMask"
    v-model="modelValue.pictureMask"
    :errors="errors.pictureMask"
  />
  <TextInput
    scope-name="note"
    field="url"
    v-model="modelValue.url"
    :errors="errors.url"
  />
  <CheckInput
    scope-name="note"
    field="urlIsVideo"
    v-model="modelValue.urlIsVideo"
    :errors="errors.urlIsVideo"
  />
  <CheckInput
    scope-name="note"
    field="skipReview"
    v-model="modelValue.skipReview"
    :errors="errors.skipReview"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import TextInput from "../form/TextInput.vue";
import CheckInput from "../form/CheckInput.vue";
import ImageInput from "../form/ImageInput.vue";

export default defineComponent({
  props: {
    modelValue: { type: Object, required: true },
    errors: {
      type: Object,
      default() {
        return {};
      },
    },
  },
  components: { TextInput, CheckInput, ImageInput },
});
</script>
