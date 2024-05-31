<template>
  <RichHtmlEditor
    v-bind="{ multipleLine, scopeName, field, title, errors }"
    :model-value="htmlValue"
    :readonly="readonly"
    @update:model-value="htmlValueUpdated($event)"
    @blur="$emit('blur')"
  />
</template>

<script lang="ts">
import { defineComponent, readonly } from "vue";
import "quill/dist/quill.snow.css";
import RichHtmlEditor from "./RichHtmlEditor.vue";
import markdownizer from "./markdownizer";

export default defineComponent({
  props: {
    multipleLine: Boolean,
    modelValue: String,
    scopeName: String,
    field: String,
    title: String,
    errors: Object,
    readonly: Boolean,
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
