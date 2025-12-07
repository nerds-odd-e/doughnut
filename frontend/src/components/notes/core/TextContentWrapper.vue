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
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { normalizeNoteDetails } from "@/utils/normalizeNoteDetails"

const storageAccessor = useStorageAccessor()

const { field, value } = defineProps({
  field: {
    type: String as PropType<"edit title" | "edit details">,
    required: true,
  },
  value: {
    type: String,
    required: false,
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
    await storageAccessor.value
      .storedApi()
      .updateTextField(noteId, field, newValue)
      .catch(errorHander)
    savedVersion.value = version
    lastSavedValue.value = newValue
  } finally {
    pendingSaveValues.delete(newValue)
  }
}
const changer = debounce(changerInner, 1000)

const localValue = ref(value)
const version = ref(0)
const errors = ref({} as Record<string, string>)

const hasUnsavedChanges = (): boolean => {
  if (field === "edit details") {
    const normalizedCurrent = normalizeNoteDetails(localValue.value ?? "")
    const normalizedSaved = normalizeNoteDetails(lastSavedValue.value ?? "")
    return normalizedCurrent !== normalizedSaved
  }
  return localValue.value !== lastSavedValue.value
}

const wrapperClass = computed(() => {
  if (hasUnsavedChanges()) {
    return "dirty"
  }
  return ""
})

const onUpdate = (noteId: number, newValue: string) => {
  if (field === "edit title" && !newValue.trim()) {
    return
  }

  if (field === "edit details") {
    const normalizedNewValue = normalizeNoteDetails(newValue)
    const normalizedLastSaved = normalizeNoteDetails(lastSavedValue.value ?? "")

    errors.value = {}
    localValue.value = newValue

    if (normalizedNewValue === normalizedLastSaved) {
      return
    }

    changer(noteId, normalizedNewValue, version.value + 1, setError)
    version.value += 1
    return
  }

  errors.value = {}
  localValue.value = newValue
  changer(noteId, newValue, version.value + 1, setError)
  version.value += 1
}

const onBlur = () => {
  changer.flush()
}

const is401Error = (errs: unknown): boolean => {
  return (
    typeof errs === "object" &&
    errs !== null &&
    "status" in errs &&
    errs.status === 401
  )
}

const setError = (errs: unknown) => {
  if (is401Error(errs)) {
    errors.value = {
      title:
        "You are not authorized to edit this note. Perhaps you are not logged in?",
    }
    return
  }

  if (typeof errs === "object" && errs !== null) {
    errors.value = errs as Record<string, string>
  } else {
    errors.value = { general: String(errs) }
  }
}

const handleNavigation = (newValue: string) => {
  changer.cancel()
  version.value = savedVersion.value
  localValue.value = newValue
  lastSavedValue.value = newValue
}

const updateToPropValue = (newValue: string | undefined) => {
  // Convert undefined to empty string for localValue (which is a string ref)
  const valueToSet = newValue ?? ""
  localValue.value = valueToSet
  lastSavedValue.value = valueToSet
}

const handlePropChange = (newValue: string | undefined) => {
  if (version.value !== savedVersion.value) {
    // Convert undefined to empty string for comparison
    const normalizedNewValue = newValue ?? ""
    if (
      normalizedNewValue !== "" &&
      pendingSaveValues.has(normalizedNewValue)
    ) {
      return
    }
    // Check if this is navigation (value changed from current or last saved)
    const normalizedCurrentValue = localValue.value ?? ""
    const normalizedLastSaved = lastSavedValue.value ?? ""
    if (
      normalizedNewValue !== normalizedCurrentValue &&
      normalizedNewValue !== normalizedLastSaved
    ) {
      handleNavigation(normalizedNewValue)
      return
    }
    return
  }
  updateToPropValue(newValue)
}

watch(() => value, handlePropChange)

onUnmounted(() => {
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
