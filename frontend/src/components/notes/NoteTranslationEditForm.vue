<template>
  <TextInput
    scopeName="note"
    field="title"
    title="Title in Indonesian"
    v-model="modelValue.title"
    :errors="errors.title"
  />
  <br />
  <TextArea
    scopeName="note"
    field="description"
    title="Description in Indonesian"
    v-model="modelValue.description"
    :errors="errors.description"
  />
  <br />
</template>

<script setup>
import TextInput from "../form/TextInput.vue";
import TextArea from "../form/TextArea.vue";

const props = defineProps({
  modelValue: { type: Object, required: true },
  errors: { type: Object, default: {} },
  components: { TextInput, TextArea },
});
</script>
