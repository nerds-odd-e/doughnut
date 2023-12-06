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
import { debounce, DebouncedFunc } from "lodash";
import EditableText from "../form/EditableText.vue";
import RichMarkdownEditor from "../form/RichMarkdownEditor.vue";
import type { StorageAccessor } from "../../store/createNoteStorage";

class SumbitChange {
  changer: DebouncedFunc<
    (
      noteId: number,
      newValue: Generated.TextContent,
      oldValue: Generated.TextContent,
      errorHander: (errs: unknown) => void,
    ) => void
  >;

  constructor(
    changer: DebouncedFunc<
      (
        noteId: number,
        newValue: Generated.TextContent,
        oldValue: Generated.TextContent,
        errorHander: (errs: unknown) => void,
      ) => void
    >,
  ) {
    this.changer = changer;
  }

  change(
    noteId: number,
    newValue: Generated.TextContent,
    oldValue: Generated.TextContent,
    errorHander: (errs: unknown) => void,
  ): void {
    this.changer(noteId, newValue, oldValue, errorHander);
  }

  flush(): void {
    this.changer.flush();
  }

  cancel(): void {
    this.changer.cancel();
  }
}

const noteTextContentChanger = (storageAccessor: StorageAccessor) => {
  return (
    noteId: number,
    newValue: Generated.TextContent,
    oldValue: Generated.TextContent,
    errorHander: (errs: unknown) => void,
  ) => {
    if (
      newValue.topic === oldValue.topic &&
      newValue.details === oldValue.details
    ) {
      return;
    }
    storageAccessor
      .storedApi()
      .updateTextContent(noteId, newValue, oldValue, errorHander);
  };
};

export default defineComponent({
  setup(props) {
    return {
      changer: new SumbitChange(
        debounce(noteTextContentChanger(props.storageAccessor), 1000),
      ),
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
      this.changer?.change(
        this.noteId,
        this.localTextContent,
        this.textContent,
        this.setError,
      );
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
