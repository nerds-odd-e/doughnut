<template>
  <slot
    :value="localValue"
    :update="onUpdate"
    :blur="onBlur"
    :errors="errors"
  />
</template>

<script lang="ts">
import { defineComponent, PropType, ref } from "vue";
import { debounce } from "lodash";
import { ApiError } from "@/generated/backend";
import BadRequestError from "@/managedApi/window/BadRequestError";
import { type StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup(props) {
    const savedVersion = ref(0);
    const changerInner = async (
      noteId: number,
      newValue: string,
      version: number,
      errorHander: (errs: unknown) => void,
    ) => {
      await props.storageAccessor
        .storedApi()
        .updateTextField(noteId, props.field, newValue)
        .catch(errorHander);
      savedVersion.value = version;
    };
    const changer = debounce(changerInner, 1000);

    return { changer, savedVersion };
  },
  props: {
    field: {
      type: String as PropType<"edit topic" | "edit details">,
      required: true,
    },
    value: {
      type: String,
      required: false,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  data() {
    return {
      localValue: this.value,
      version: 0,
      errors: {} as Record<string, string>,
    };
  },

  methods: {
    onUpdate(noteId: number, newValue: string) {
      this.version += 1;
      this.errors = {};
      this.localValue = newValue;
      this.changer(noteId, newValue, this.version, this.setError);
    },
    onBlur() {
      this.changer.flush();
    },
    setError(errs: unknown) {
      if (errs instanceof ApiError) {
        if (errs.status === 401) {
          this.errors = {
            topic:
              "You are not authorized to edit this note. Perhaps you are not logged in?",
          };
          return;
        }
      }
      if (errs instanceof BadRequestError) {
        this.errors = errs as unknown as Record<string, string>;
      }
    },
  },
  watch: {
    value() {
      if (this.version !== this.savedVersion) {
        return;
      }
      this.localValue = this.value;
    },
  },
  unmounted() {
    this.changer.flush();
    this.changer.cancel();
  },
});
</script>
