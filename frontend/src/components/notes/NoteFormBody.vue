<template>
  <ImageInput
    scopeName="note"
    field="uploadPicture"
    placeholder="Optional. upload own picture."
    :errors="errors.uploadPictureProxy"
    v-model="modelValue.uploadPictureProxy"
  />
  <TextInput
    scopeName="note"
    field="pictureUrl"
    placeholder="Full url of existing picture."
    :errors="errors.pictureUrl"
    v-model="modelValue.pictureUrl"
  />
  <CheckInput
    scopeName="note"
    field="useParentPicture"
    v-model="modelValue.useParentPicture"
    :errors="errors.useParentPicture"
  />
  <TextInput
    scopeName="note"
    field="pictureMask"
    v-model="modelValue.pictureMask"
    :errors="errors.pictureMask"
  />
  <TextInput
    scopeName="note"
    field="url"
    v-model="modelValue.url"
    :errors="errors.url"
  />
  <CheckInput
    scopeName="note"
    field="urlIsVideo"
    v-model="modelValue.urlIsVideo"
    :errors="errors.urlIsVideo"
  />
  <CheckInput
    scopeName="note"
    field="skipReview"
    v-model="modelValue.skipReview"
    :errors="errors.skipReview"
  />
</template>

<script setup>
import TextInput from "../form/TextInput.vue";
import CheckInput from "../form/CheckInput.vue";
import ImageInput from "../form/ImageInput.vue";

const props = defineProps({
  modelValue: { type: Object, required: true },
  errors: { type: Object, default: {} },
});
</script>
