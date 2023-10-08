<template>
  <div style="display: flex">
    <EditableText
      role="topic"
      class="note-topic"
      scope-name="note"
      :model-value="localTextContent.topic"
      @update:model-value="onUpdateTitle"
      @blur="onBlurTextField"
      :errors="errors.topic"
    />
    <slot name="topic-additional" />
  </div>
  <div class="note-content">
    <RichMarkdownEditor
      :multiple-line="true"
      scope-name="note"
      :model-value="localTextContent.details"
      @update:model-value="onUpdateDetails"
      @blur="onBlurTextField"
    />
    <slot name="note-content-other" />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { debounce, DebouncedFunc } from "lodash";
import EditableText from "../form/EditableText.vue";
import RichMarkdownEditor from "../form/RichMarkdownEditor.vue";
import type { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return {
      submitChange: null as DebouncedFunc<
        (newValue: Generated.TextContent) => void
      > | null,
    };
  },
  props: {
    noteId: { type: Number, required: true },
    textContent: {
      type: Object as PropType<Generated.TextContent>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    EditableText,
    RichMarkdownEditor,
  },
  data() {
    return {
      localTextContent: { ...this.textContent } as Generated.TextContent,
      errors: {} as Record<string, string>,
      revision: 0,
    };
  },
  watch: {
    textContent: {
      handler(newValue) {
        if (this.revision !== 0) return;
        this.localTextContent = { ...newValue };
      },
      deep: true,
    },
  },
  methods: {
    onUpdateTitle(newValue: string) {
      this.localTextContent.topic = newValue;
      this.saveChange();
    },
    onUpdateDetails(newValue: string) {
      this.localTextContent.details = newValue;
      this.saveChange();
    },
    saveChange() {
      this.revision += 1;
      this.errors = {};
      if (!this.submitChange) {
        return;
      }
      this.submitChange(this.localTextContent);
    },
    onBlurTextField() {
      if (!this.submitChange) {
        return;
      }
      this.submitChange.flush();
    },
    isMeaningfulChange(newValue: Generated.TextContent) {
      return (
        newValue.topic !== this.textContent.topic ||
        newValue.details !== this.textContent.details
      );
    },
  },
  mounted() {
    this.submitChange = debounce((newValue: Generated.TextContent) => {
      if (!this.isMeaningfulChange(newValue)) {
        return;
      }
      const currentRivision = this.revision;
      this.storageAccessor
        .api(this.$router)
        .updateTextContent(this.noteId, newValue, this.textContent)
        .catch((errors) => {
          if (errors.status === 401) {
            this.errors = {
              topic:
                "You are not authorized to edit this note. Perhaps you are not logged in?",
            };
            return;
          }
          this.errors = errors;
        })
        .finally(() => {
          if (this.revision !== currentRivision) return;
          this.revision = 0;
        });
    }, 1000);
  },
  unmounted() {
    if (this.submitChange) {
      this.submitChange.flush();
      this.submitChange.cancel();
    }
  },
});
</script>
