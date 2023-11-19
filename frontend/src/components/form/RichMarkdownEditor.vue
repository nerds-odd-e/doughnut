<template>
  <RichHtmlEditor
    v-bind="{ multipleLine, scopeName, field, title, errors }"
    v-model="localHtmlValue"
    @blur="$emit('blur')"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { marked } from "marked";
import "quill/dist/quill.snow.css";
import TurndownService from "turndown";
import RichHtmlEditor from "./RichHtmlEditor.vue";

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
    RichHtmlEditor,
  },
  data() {
    return {
      localHtmlValue: markdownizer.markdownToHtml(this.modelValue),
    };
  },
  watch: {
    modelValue() {
      this.localHtmlValue = markdownizer.markdownToHtml(this.modelValue);
    },
    localHtmlValue() {
      const localMarkdownValue = markdownizer.htmlToMarkdown(
        this.localHtmlValue,
      );
      this.$emit("update:modelValue", localMarkdownValue);
    },
  },
});
</script>
