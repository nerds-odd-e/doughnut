<template>
  <RichHtmlEditor
    v-bind="{ multipleLine, scopeName, field, title, errors }"
    :model-value="htmlValue"
    @update:model-value="htmlValueUpdated($event)"
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
  computed: {
    htmlValue() {
      return markdownizer.markdownToHtml(this.modelValue);
    },
  },
  methods: {
    htmlValueUpdated(htmlValue) {
      const markdownValue = markdownizer.htmlToMarkdown(htmlValue);
      this.$emit("update:modelValue", markdownValue);
    },
  },
});
</script>
