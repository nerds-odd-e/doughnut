<template>
  <div style="display: flex">
    <EditableText
      role="title"
      class="note-title"
      scope-name="note"
      v-model="localTextContent.title"
      @blur="onBlurTextField"
    />
    <slot name="title-additional" />
  </div>
  <div class="note-content">
    <DescriptionEditor
      :multiple-line="true"
      role="description"
      v-if="size === 'large'"
      class="note-description"
      scope-name="note"
      v-model="localTextContent.description"
      @blur="onBlurTextField"
    />
    <NoteShortDescription
      v-if="size === 'medium'"
      :description="localTextContent.description"
    />
    <SvgDescriptionIndicator
      v-if="size === 'small' && !!localTextContent.description"
      class="description-indicator"
    />
    <slot name="note-content-other" />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { debounce, DebouncedFunc } from "lodash";
import NoteShortDescription from "./NoteShortDescription.vue";
import SvgDescriptionIndicator from "../svgs/SvgDescriptionIndicator.vue";
import EditableText from "../form/EditableText.vue";
import DescriptionEditor from "../form/DescriptionEditor.vue";
import type { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return {
      submitChange: null as DebouncedFunc<() => void> | null,
    };
  },
  props: {
    noteId: { type: Number, required: true },
    textContent: {
      type: Object as PropType<Generated.TextContent>,
      required: true,
    },
    size: { type: String, default: "large" },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    NoteShortDescription,
    SvgDescriptionIndicator,
    EditableText,
    DescriptionEditor,
  },
  data() {
    return {
      localTextContent: { ...this.textContent } as Generated.TextContent,
    };
  },
  watch: {
    textContent1(newValue) {
      this.localTextContent = { ...newValue };
    },
  },
  methods: {
    onBlurTextField() {
      if (!this.submitChange) {
        return;
      }
      this.submitChange();
      this.submitChange.flush();
    },
  },
  mounted() {
    this.submitChange = debounce(() => {
      this.storageAccessor
        .api(this.$router)
        .updateTextContent(
          this.noteId,
          this.localTextContent,
          this.textContent
        );
    }, 1000);
  },
});
</script>
