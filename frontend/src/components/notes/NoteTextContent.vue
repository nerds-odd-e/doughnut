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
      newValue: Generated.TextContent,
      oldValue: Generated.TextContent,
      errorHander: (errs: unknown) => void,
    ) => void
  >;

  constructor(
    changer: DebouncedFunc<
      (
        newValue: Generated.TextContent,
        oldValue: Generated.TextContent,
        errorHander: (errs: unknown) => void,
      ) => void
    >,
  ) {
    this.changer = changer;
  }

  change(
    newValue: Generated.TextContent,
    oldValue: Generated.TextContent,
    errorHander: (errs: unknown) => void,
  ): void {
    this.changer(newValue, oldValue, errorHander);
  }
}

export default defineComponent({
  setup() {
    return {
      submitChange: null as DebouncedFunc<
        (
          newValue: Generated.TextContent,
          oldValue: Generated.TextContent,
          errorHander: (errs: unknown) => void,
        ) => void
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
      changer: null as SumbitChange | null,
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
      if (!this.changer) {
        return;
      }
      this.changer.change(
        this.localTextContent,
        this.textContent,
        this.setError,
      );
    },
    onBlurTextField() {
      if (!this.submitChange) {
        return;
      }
      this.submitChange.flush();
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
  mounted() {
    const changer = (
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
      this.storageAccessor
        .api(this.$router)
        .updateTextContent(this.noteId, newValue, oldValue, errorHander);
    };
    this.submitChange = debounce(changer, 1000);
    this.changer = new SumbitChange(this.submitChange);
  },
  unmounted() {
    if (this.submitChange) {
      this.submitChange.flush();
      this.submitChange.cancel();
    }
  },
});
</script>
