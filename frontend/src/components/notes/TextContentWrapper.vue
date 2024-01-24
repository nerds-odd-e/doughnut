<template>
  <slot :update="onUpdate" :blur="onBlur" :errors="errors" />
</template>

<script setup lang="ts">
import { ref, onUnmounted, PropType } from "vue";
import { debounce } from "lodash";
import type { StorageAccessor } from "../../store/createNoteStorage";

const props = defineProps({
  field: {
    type: String as PropType<"edit topic" | "edit details">,
    required: true,
  },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
});

const errors = ref({} as Record<string, string>);

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const setError = (errs: any) => {
  if (errs.status === 401) {
    errors.value = {
      topic:
        "You are not authorized to edit this note. Perhaps you are not logged in?",
    };
    return;
  }
  errors.value = errs;
};

const changerInner = (
  noteId: number,
  newValue: string,
  errorHandler: (errs: unknown) => void,
) => {
  props.storageAccessor
    .storedApi()
    .updateTextField(noteId, props.field, newValue)
    .catch(errorHandler);
};

const changer = debounce(changerInner, 1000);

const onUpdate = (noteId: number, newValue: string) => {
  errors.value = {};
  changer(noteId, newValue, setError);
};

const onBlur = () => {
  changer.flush();
};

onUnmounted(() => {
  changer.flush();
  changer.cancel();
});
</script>
