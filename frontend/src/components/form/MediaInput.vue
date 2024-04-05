<template>
  <InputWithType v-bind="{ scopeName, field, errors, accept, title }">
    <input
      :class="`file-input-control form-control`"
      :id="`${scopeName}-${field}`"
      type="file"
      :accept="`${accept}`"
      :name="field"
      @change="update($event.target.files[0])"
      :placeholder="placeholder"
      :autofocus="autofocus"
      autocomplete="off"
      autocapitalize="off"
      v-if="title !== undefined"
      :title="`${title}`"
    />
    <input
      :class="`file-input-control form-control ${!!errors ? 'is-invalid' : ''}`"
      :id="`${scopeName}-${field}`"
      type="file"
      :accept="`${accept}`"
      :name="field"
      @change="update($event.target.files[0])"
      :placeholder="placeholder"
      :autofocus="autofocus"
      autocomplete="off"
      autocapitalize="off"
      v-else
    />
  </InputWithType>
</template>

<script>
import InputWithType from "./InputWithType.vue";

export default {
  props: {
    modelValue: String,
    scopeName: String,
    field: String,
    accept: { type: String, default: "*" },
    title: { type: String, default: undefined },
    placeholder: { type: String, default: null },
    autofocus: { type: Boolean, default: false },
    errors: Object,
  },
  emits: ["update:modelValue"],
  components: { InputWithType },
  methods: {
    update(data) {
      this.$emit("update:modelValue", data);
    },
  },
};
</script>
