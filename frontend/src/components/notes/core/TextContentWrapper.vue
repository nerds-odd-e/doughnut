<template>
  <div :class="wrapperClass">
    <slot
      :value="localValue"
      :update="onUpdate"
      :blur="onBlur"
      :errors="errors"
    />
  </div>
</template>

<script setup lang="ts">
import { debounce } from "es-toolkit"
import type { PropType } from "vue"
import { computed, onUnmounted, ref, watch } from "vue"
import { type StorageAccessor } from "../../../store/createNoteStorage"

const { storageAccessor, field, value } = defineProps({
  field: {
    type: String as PropType<"edit title" | "edit details">,
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
})

const savedVersion = ref(0)
const lastSavedValue = ref(value)
const pendingSaveValues = new Set<string>()
const changerInner = async (
  noteId: number,
  newValue: string,
  version: number,
  errorHander: (errs: unknown) => void
) => {
  pendingSaveValues.add(newValue)
  try {
    await storageAccessor
      .storedApi()
      .updateTextField(noteId, field, newValue)
      .catch(errorHander)
    savedVersion.value = version
    lastSavedValue.value = newValue
  } finally {
    pendingSaveValues.delete(newValue)
  }
}
// Debounced executor for auto-save
const changer = debounce(changerInner, 1000)

const localValue = ref(value)
const version = ref(0)
const errors = ref({} as Record<string, string>)

const wrapperClass = computed(() => {
  if (version.value !== savedVersion.value) {
    return "dirty"
  }
  return ""
})

const onUpdate = (noteId: number, newValue: string) => {
  if (field === "edit title" && !newValue.trim()) {
    // Do not update or schedule save for blank titles
    return
  }
  version.value += 1
  errors.value = {}
  localValue.value = newValue
  // Schedule auto-save with debouncing
  changer(noteId, newValue, version.value, setError)
}

const onBlur = () => {
  // Flush any pending debounced save immediately
  changer.flush()
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const setError = (errs: unknown) => {
  if (typeof errs === "object" && errs !== null && "status" in errs) {
    if (errs.status === 401) {
      errors.value = {
        title:
          "You are not authorized to edit this note. Perhaps you are not logged in?",
      }
      return
    }
  }

  if (typeof errs === "object" && errs !== null) {
    errors.value = errs as Record<string, string>
  } else {
    errors.value = { general: String(errs) }
  }
}

watch(
  () => value,
  (newValue) => {
    if (version.value !== savedVersion.value) {
      // There are unsaved changes
      // If the incoming value matches a pending save, ignore it to preserve newer edits
      if (newValue !== undefined && pendingSaveValues.has(newValue)) {
        return
      }
      // Check if this is navigation to a different note or just API returning with saved value
      if (newValue !== localValue.value && newValue !== lastSavedValue.value) {
        // The incoming value is different from both current and last saved value
        // This indicates navigation to a different note
        changer.cancel()
        version.value = savedVersion.value
        localValue.value = newValue
        lastSavedValue.value = newValue
      }
      // Otherwise, keep the unsaved changes
      // This handles the case where API returns with a previously saved value
      // but user has made newer edits that haven't been saved yet
      return
    }
    // No unsaved changes, update to match the prop
    localValue.value = newValue
    lastSavedValue.value = newValue
  }
)

onUnmounted(() => {
  // Flush any pending debounced save before unmounting
  changer.flush()
})
</script>

<style lang="sass">
.dirty
  position: relative
  background-color: transparent
  &::after
    content: ""
    position: absolute
    top: 0
    right: 0
    border-top: 5px solid transparent
    border-left: 5px solid transparent
    border-right: 5px solid red
    border-bottom: 5px solid red
    transform: rotate(-90deg)
    z-index: 1000

  </style>
