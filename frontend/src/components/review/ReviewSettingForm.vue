<template>
  <TextInput
    v-if="showLevel"
    scope-name="review_setting"
    field="level"
    :model-value="modelValue.level"
    :errors="errors.level"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, level: $event })
    "
  />
  <CheckInput
    scope-name="review_setting"
    field="rememberSpelling"
    :model-value="modelValue.rememberSpelling"
    :errors="errors.rememberSpelling"
    @update:model-value="
      $emit('update:modelValue', { ...modelValue, rememberSpelling: $event })
    "
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import CheckInput from "../form/CheckInput.vue";
import TextInput from "../form/TextInput.vue";

export default defineComponent({
  props: { modelValue: Object, errors: Object, showLevel: Boolean },
  components: { CheckInput, TextInput },
  emits: ["update:modelValue"],
});
</script>
