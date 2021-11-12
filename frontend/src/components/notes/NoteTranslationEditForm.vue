<template>
  <TextInput
    scopeName="note"
    field="titleIDN"
    title="Title in Indonesian"
    v-model="modelValue.titleIDN"
    :errors="errors.title"
  />
  <br />
  <TextArea
    scopeName="note"
    field="descriptionIDN"
    title="Description in Indonesian"
    v-model="modelValue.descriptionIDN"
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
