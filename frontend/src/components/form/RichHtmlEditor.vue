<template>
  <QuillEditor
    ref="quillEditor"
    v-model:content="localValue"
    :options="editorOptions"
    :content-type="'html'"
    @blur="onBlurTextField"
    @update:content="onUpdateContent"
    @focus="hadFocus = true"
    @selection-change="setSelection"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { QuillEditor } from "@vueup/vue-quill";

import "quill/dist/quill.snow.css";
import { ref } from "vue";

const quillEditor = ref();

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
        placeholder: "Enter note details here...",
      },
      localValue: this.modelValue,
      hadFocus: false as boolean,
    };
  },
  watch: {
    modelValue() {
      this.localValue = this.modelValue;
    },
  },
  methods: {
    onUpdateContent() {
      if (this.localValue === this.modelValue) {
        return;
      }
      this.$emit("update:modelValue", this.localValue);
    },
    onBlurTextField() {
      alert('Blur')
      this.$emit("blur");
    },
    setSelection(data) {
        const range = data.range;
        // remove html tag
        const textContent = this.localValue?.replaceAll('<p>', '').replaceAll('</p>', '');
        const selectedValue = textContent.substring(range.index, range.index + range.length)
        alert('Range: ' + selectedValue)
    }
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
  li
    list-style-type: inherit
.ql-container.ql-snow
  border: none
  font-size: inherit !important
</style>
