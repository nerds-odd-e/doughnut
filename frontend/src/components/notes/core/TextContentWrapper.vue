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
const changer = debounce(changerInner, 5000)

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
  version.value += 1
  errors.value = {}
  localValue.value = newValue
  changer(noteId, newValue, version.value, setError)
}

const onBlur = () => {
  changer.flush()
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const setError = (errs: unknown) => {
  if (typeof errs === "object" && errs !== null && "status" in errs) {
    if (errs.status === 401) {
      errors.value = {
        topic:
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
  () => {
    if (version.value !== savedVersion.value) {
      return
    }
    localValue.value = value
  }
)

onUnmounted(() => {
  changer.flush()
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
