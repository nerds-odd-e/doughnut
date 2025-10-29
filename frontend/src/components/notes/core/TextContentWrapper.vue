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
const changerInner = async (
  noteId: number,
  newValue: string,
  version: number,
  errorHander: (errs: unknown) => void
) => {
  await storageAccessor
    .storedApi()
    .updateTextField(noteId, field, newValue)
    .catch(errorHander)
  savedVersion.value = version
}
// Debounced executor used only when explicitly triggered (on blur/unmount)
const changer = debounce(changerInner, 1000)

const localValue = ref(value)
const version = ref(0)
const errors = ref({} as Record<string, string>)
// Track the latest edited value and note id without auto-saving
const latestNoteId = ref<number | null>(null)
const latestValue = ref<string | null>(null)

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
  // Record latest edit, but do not auto-save yet
  latestNoteId.value = noteId
  latestValue.value = newValue
}

const onBlur = () => {
  // Trigger a save for the latest edit and flush immediately
  if (latestNoteId.value != null && latestValue.value != null) {
    changer(latestNoteId.value, latestValue.value, version.value, setError)
    changer.flush()
  }
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
      // Only reset if the incoming value is different from what we're currently showing
      // This indicates navigation to a different note
      if (newValue !== localValue.value) {
        // Cancel any pending saves when navigating away with unsaved changes
        changer.cancel()
        // Reset the tracking state
        latestNoteId.value = null
        latestValue.value = null
        // Reset version tracking
        version.value = savedVersion.value
        localValue.value = newValue
      }
      // Otherwise, keep the unsaved changes (same note, just prop update)
      return
    }
    localValue.value = newValue
  }
)

onUnmounted(() => {
  // If there is a pending edit, schedule and flush to persist it
  if (latestNoteId.value != null && latestValue.value != null) {
    changer(latestNoteId.value, latestValue.value, version.value, setError)
    changer.flush()
  }
  changer.cancel()
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
