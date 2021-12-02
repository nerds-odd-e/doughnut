<template>
  <TextInput
    v-if="showLevel"
    scopeName="review_setting"
    field="level"
    v-model="modelValue.level"
    :errors="errors.level"
  />
  <CheckInput
    scopeName="review_setting"
    field="rememberSpelling"
    v-model="modelValue.rememberSpelling"
    :errors="errors.rememberSpelling"
  />
</template>

<script>
import CheckInput from "../form/CheckInput.vue";
import TextInput from "../form/TextInput.vue";
export default {
  props: { modelValue: Object, errors: Object, showLevel: Boolean },
  components: { CheckInput, TextInput },
};
</script>
