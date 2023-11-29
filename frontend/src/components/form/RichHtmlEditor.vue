<template>
  <div class="editor">
    <p class="suggestion">
      <span class="suggestion-local">{{ simpleText }}</span>
      <span class="suggestion-text">{{ suggestion }}</span>
    </p>
    <QuillEditor
      class="quill-editor"
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
import useLoadingApi from "@/managedApi/useLoadingApi";

import "quill/dist/quill.snow.css";

export default defineComponent({
  setup() {
    return {
      ...useLoadingApi(),
    };
  },
  props: {
    multipleLine: Boolean,
    modelValue: String,
    scopeName: String,
    field: String,
    title: String,
    errors: Object,
    noteId: { type: Number, required: true },
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
    setSimpleText() {
      const quill = this.$refs.quillEditor as typeof QuillEditor;
      this.simpleText = quill.getText();
    },
    async onEnterPress() {
      const quill = this.$refs.quillEditor as typeof QuillEditor;

      if (this.suggestion) {
        const finalText = `<p>${this.simpleText} ${this.suggestion}</p>`;
        await quill.setHTML(finalText);
        quill.getQuill().setSelection(quill.getQuill().getLength(), 0);
      } else {
        quill
          .getQuill()
          .insertText(quill.getQuill().getSelection()?.index || 0, "\n");
      }
    },
    async onSpacePress() {
      this.setSimpleText();
      const quill = this.$refs.quillEditor as typeof QuillEditor;
      quill
        .getQuill()
        .insertText(quill.getQuill().getSelection()?.index || 0, " ");

      const response = await this.api.ai.aiNoteDetailsCompletion(this.noteId, {
        detailsToComplete: this.simpleText.trim(),
        clarifyingQuestionAndAnswers: [],
      });
      const fixedSuggestion = response.moreCompleteContent.substring(
        this.simpleText.length - 1,
      );
      this.suggestion = fixedSuggestion;
    },
  },
});
</script>

<style lang="sass">
.ql-editor
  padding: 0 0 40px 0
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
  font-family: Helvetica, Arial, sans-serif
.suggestion-local
  color: transparent
  pointer-events: none
</style>
