<template>
  <TextInput
    scopeName="note"
    field="title"
    v-model="modelValue.title"
    :errors="errors.title"
  />
</template>

<script setup>
import TextInput from "../form/TextInput.vue";

const props = defineProps({
  modelValue: { type: Object, required: true },
  errors: { type: Object, default: {} },
});
</script>
