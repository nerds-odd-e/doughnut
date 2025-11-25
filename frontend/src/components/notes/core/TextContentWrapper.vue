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
    return
  }
  version.value += 1
  errors.value = {}
  localValue.value = newValue
  changer(noteId, newValue, version.value, setError)
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

const isNavigation = (newValue: string | undefined): newValue is string => {
  return (
    newValue !== undefined &&
    newValue !== localValue.value &&
    newValue !== lastSavedValue.value
  )
}

const handleNavigation = (newValue: string) => {
  changer.cancel()
  version.value = savedVersion.value
  localValue.value = newValue
  lastSavedValue.value = newValue
}

const updateToPropValue = (newValue: string | undefined) => {
  if (newValue !== undefined) {
    localValue.value = newValue
    lastSavedValue.value = newValue
  }
}

const handlePropChange = (newValue: string | undefined) => {
  if (version.value !== savedVersion.value) {
    if (newValue !== undefined && pendingSaveValues.has(newValue)) {
      return
    }
    if (isNavigation(newValue)) {
      handleNavigation(newValue)
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
