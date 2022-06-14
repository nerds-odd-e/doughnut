<template>
  <TextInput
    scope-name="note"
    field="title"
    :model-value="modelValue.title"
    :errors="errors.title"
    v-focus
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, title: $event })
    "
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import TextInput from "../form/TextInput.vue";

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
  emits: ["update:modelValue"],
  components: { TextInput },
});

// test push to github
</script>
