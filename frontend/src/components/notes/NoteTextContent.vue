<template>
  <div style="display: flex">
    <EditableText
      role="title"
      class="note-title"
      scope-name="note"
      v-model="localTextContent.title"
      @blur="onBlurTextField"
      :errors="errors.title"
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
      errors: {} as Record<string, string>,
    };
  },
  watch: {
    textContent: {
      handler(newValue) {
        this.localTextContent = { ...newValue };
      },
      deep: true,
    },
    localTextContent: {
      handler(newValue) {
        this.errors = {};
        if (!this.submitChange) {
          return;
        }
        this.submitChange(newValue);
      },
      deep: true,
    },
  },
  methods: {
    onBlurTextField() {
      if (!this.submitChange) {
        return;
      }
      this.submitChange.flush();
    },
    isMeaningfulChange(newValue: Generated.TextContent) {
      return (
        newValue.title !== this.textContent.title ||
        !(
          newValue.description === this.textContent.description ||
          newValue.description === `<p>${this.textContent.description}</p>`
        )
      );
    },
  },
  mounted() {
    this.submitChange = debounce((newValue: Generated.TextContent) => {
      if (!this.isMeaningfulChange(newValue)) {
        return;
      }
      this.storageAccessor
        .api(this.$router)
        .updateTextContent(this.noteId, newValue, this.textContent)
        .catch((errors) => {
          if (errors.status === 401) {
            this.errors = {
              title:
                "You are not authorized to edit this note. Perhaps you are not logged in?",
            };
            return;
          }
          this.errors = errors;
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
