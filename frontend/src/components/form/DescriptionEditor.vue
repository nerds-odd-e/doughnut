<template>
  <QuillEditor
    ref="quillEditor"
    v-model:content="localValue"
    :options="editorOptions"
    :content-type="'text'"
    @blur="onBlurTextField"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { QuillEditor } from "@vueup/vue-quill";

import "quill/dist/quill.snow.css";

export default defineComponent({
  props: {
    multipleLine: Boolean,
    modelValue: String,
    scopeName: String,
    field: String,
    title: String,
    errors: Object,
  },
  emits: ["update:modelValue", "blur"],
  components: {
    QuillEditor,
  },
  data() {
    return {
      editorOptions: {
        modules: {
          toolbar: false,
          keyboard: {
            bindings: {
              custom: {
                key: 13,
                shiftKey: true,
                handler: this.onBlurTextField,
              },
            },
          },
        },
        placeholder: "Enter note description here...",
      },
      initialValue: this.modelValue || ("" as string),
      localValue: this.modelValue || ("" as string),
    };
  },
  watch: {
    modelValue() {
      if (this.localValue !== this.modelValue) {
        this.localValue = this.modelValue || ("" as string);
      }
    },
  },
  methods: {
    onBlurTextField() {
      if (this.initialValue.trim() !== this.localValue.trim()) {
        this.initialValue = this.localValue;
        this.$emit("update:modelValue", this.localValue);
        this.$emit("blur");
      }
    },
  },
  beforeUnmount() {
    this.onBlurTextField();
  },
});
</script>
