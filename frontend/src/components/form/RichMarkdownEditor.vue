<template>
  <QuillEditor
    ref="quillEditor"
    v-model:content="localHtmlValue"
    :options="editorOptions"
    :content-type="'html'"
    @blur="onBlurTextField"
    @update:content="onUpdateContent"
    @focus="hadFocus = true"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { marked } from "marked";
import { QuillEditor } from "@vueup/vue-quill";

import "quill/dist/quill.snow.css";
import TurndownService from "turndown";

const turndownService = new TurndownService();

const markdownizer = {
  markdownToHtml(markdown: string | undefined) {
    return marked(markdown || "")
      .trim()
      .replace(/>\s+</g, "><");
  },
  htmlToMarkdown(html: string) {
    return turndownService.turndown(html);
  },
};

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
      localHtmlValue: markdownizer.markdownToHtml(this.modelValue),
      hadFocus: false as boolean,
    };
  },
  watch: {
    modelValue() {
      this.localHtmlValue = markdownizer.markdownToHtml(this.modelValue);
    },
  },
  computed: {
    localMarkdownValue() {
      return markdownizer.htmlToMarkdown(this.localHtmlValue);
    },
  },
  methods: {
    onUpdateContent() {
      if (this.localMarkdownValue === this.modelValue) {
        return;
      }
      this.$emit("update:modelValue", this.localMarkdownValue);
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
  li
    list-style-type: inherit
.ql-container.ql-snow
  border: none
  font-size: inherit !important
</style>
