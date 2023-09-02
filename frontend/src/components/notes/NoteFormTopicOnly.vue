<template>
  <TextInput
    scope-name="note"
    field="topic"
    :model-value="modelValue.topic"
    :errors="errors.topic"
    v-focus
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, topic: $event })
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
</script>
