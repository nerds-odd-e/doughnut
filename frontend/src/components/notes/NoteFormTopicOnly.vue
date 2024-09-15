<template>
  <TextInput
    scope-name="note"
    field="topic"
    :model-value="modelValue"
    :error-message="errorMessage"
    v-focus
    @update:model-value="$emit('update:modelValue', $event)"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue"
import TextInput from "../form/TextInput.vue"

export default defineComponent({
  props: {
    modelValue: { type: String },
    errorMessage: { type: String },
  },
  emits: ["update:modelValue"],
  components: { TextInput },
})
</script>
