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
  <div role="details" class="note-content">
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
import { debounce } from "lodash";
import EditableText from "../form/EditableText.vue";
import RichMarkdownEditor from "../form/RichMarkdownEditor.vue";
import {
  NoteTextContentChanger,
  type StorageAccessor,
} from "../../store/createNoteStorage";

export default defineComponent({
  setup(props) {
    const changerInner = async (
      noteId: number,
      newValue: Generated.TextContent,
      errorHander: (errs: unknown) => void,
    ) => {
      try {
        const currentNote =
          props.storageAccessor.refOfNoteRealm(noteId).value?.note;
        const field =
          currentNote?.topic !== newValue.topic ? "edit topic" : "edit details";
        const value =
          currentNote?.topic !== newValue.topic
            ? newValue.topic
            : newValue.details;
        if (currentNote) {
          const old: Generated.TextContent = {
            topic: currentNote.topic,
            details: currentNote.details,
          };

          if (
            old.topic === newValue.topic &&
            old.details === newValue.details
          ) {
            return;
          }
        }
        await props.storageAccessor
          .storedApi()
          .updateTextField(noteId, field, value);
      } catch (e) {
        errorHander(e);
      }
    };
    const changer = new NoteTextContentChanger(debounce(changerInner, 1000));

    return { changer };
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
    };
  },
  watch: {
    textContent: {
      handler(newValue) {
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
      this.errors = {};
      this.changer?.change(this.noteId, this.localTextContent, this.setError);
    },
    onBlurTextField() {
      this.changer?.flush();
    },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    setError(errs: any) {
      if (errs.status === 401) {
        this.errors = {
          topic:
            "You are not authorized to edit this note. Perhaps you are not logged in?",
        };
        return;
      }
      this.errors = errs;
    },
  },
  unmounted() {
    this.changer?.flush();
    this.changer?.cancel();
  },
});
</script>
