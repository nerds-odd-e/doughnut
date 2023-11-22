<template>
  <div class="editor">
    <p class="suggestion">
      <span class="suggestion-local">{{ simpleText }}</span>
      <span class="suggestion-text">{{ suggestion }}</span>
    </p>
    <QuillEditor
      ref="quillEditor"
      v-model:content="localValue"
      :options="editorOptions"
      :content-type="'html'"
      @blur="onBlurTextField"
      @update:content="onUpdateContent"
      @focus="hadFocus = true"
    />
  </div>
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
              space: {
                key: 32,
                shiftKey: false,
                handler: this.onSpacePress,
              },
              enter: {
                key: 13,
                shiftKey: false,
                handler: this.onEnterPress,
              },
            },
          },
        },
        placeholder: "Enter note details here...",
      },
      localValue: this.modelValue,
      hadFocus: false as boolean,
      suggestion: "",
      simpleText: "",
    };
  },
  watch: {
    modelValue() {
      this.localValue = this.modelValue;
      const quill = this.$refs.quillEditor as typeof QuillEditor;
      this.simpleText = quill.getText();
      this.suggestion = "";
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
      this.$emit("blur");
    },
    onEnterPress() {
      const quill = this.$refs.quillEditor as typeof QuillEditor;

      if (this.suggestion) {
        const finalText = `<p>${this.simpleText} ${this.suggestion}</p>`;
        quill.setHTML(finalText);
        quill.getQuill().setSelection(quill.getQuill().getLength(), 0);
      } else {
        quill
          .getQuill()
          .insertText(quill.getQuill().getSelection()?.index || 0, "\n");
      }
    },
    onSpacePress() {
      const quill = this.$refs.quillEditor as typeof QuillEditor;
      quill
        .getQuill()
        .insertText(quill.getQuill().getSelection()?.index || 0, " ");
      if (this.modelValue === "<p>Schroedinger-Team: Scrum</p>") {
        this.suggestion = "is a popular Software Development Framework.";
      } else {
        this.suggestion = "";
      }
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
    right: 0 !important
  li
    list-style-type: inherit
.ql-container.ql-snow
  border: none
  font-size: inherit !important

.editor
  position: relative
.suggestion
  position: absolute
  top: 0
  left: 0
  color: grey
  line-height: 1.42
  word-wrap: break-word
.suggestion-local
  color: transparent
  pointer-events: none
</style>
