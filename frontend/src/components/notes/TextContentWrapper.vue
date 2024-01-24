<template>
  <slot :update="onUpdate" :blur="onBlur" :errors="errors" />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { debounce } from "lodash";
import { type StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup(props) {
    const changerInner = (
      noteId: number,
      newValue: string,
      errorHander: (errs: unknown) => void,
    ) => {
      props.storageAccessor
        .storedApi()
        .updateTextField(noteId, props.field, newValue)
        .catch(errorHander);
    };
    const changer = debounce(changerInner, 1000);

    return { changer };
  },
  props: {
    field: {
      type: String as PropType<"edit topic" | "edit details">,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  data() {
    return {
      errors: {} as Record<string, string>,
    };
  },

  methods: {
    onUpdate(noteId: number, newValue: string) {
      this.errors = {};
      this.changer(noteId, newValue, this.setError);
    },
    onBlur() {
      this.changer.flush();
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
    this.changer.flush();
    this.changer.cancel();
  },
});
</script>
