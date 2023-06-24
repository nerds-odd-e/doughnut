<template>
  <QuillEditor
    ref="quillEditor"
    v-model:content="localValue"
    :options="editorOptions"
    :content-type="'html'"
    @blur="onBlurTextField"
    @update:content="onTextChange"
    @focus="hadFocus = true"
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
      localValue: this.modelValue || ("" as string),
      hadFocus: false as boolean,
    };
  },
  watch: {
    modelValue() {
      this.localValue = this.modelValue || ("" as string);
    },
  },
  methods: {
    onTextChange() {
      this.$emit("update:modelValue", this.localValue);
    },
    onBlurTextField() {
      this.$emit("blur");
    },
  },
});
</script>

<style lang="sass">
.ql-editor
  padding: 0
  margin-bottom: 15px
  &::before
    left: 0 !important
    right:0 !important
.ql-container.ql-snow
  border: none
  font-size: inherit !important
</style>
