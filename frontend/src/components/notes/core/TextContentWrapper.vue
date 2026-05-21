<template>
  <div :class="wrapperClass" @focusout="onReferencedTitleFocusOut">
    <slot
      :value="localValue"
      :update="onUpdate"
      :blur="onBlur"
      :errors="errors"
    />
    <div
      v-if="showReferencedTitleSavePanel"
      class="referenced-title-save mt-3 flex flex-col gap-3"
      data-testid="referenced-title-save-panel"
    >
      <p class="text-sm opacity-80">
        This note is linked from other notes. Choose how wiki links to this note
        should change:
      </p>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="opt in TITLE_RENAME_REFERENCE_SAVE_OPTIONS"
          :key="opt.value"
          type="button"
          class="daisy-btn daisy-btn-primary daisy-btn-sm"
          :data-testid="opt.testid"
          :disabled="savingReferencedTitle"
          @mousedown.prevent
          @click="saveReferencedTitleWithChoice(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { debounce } from "es-toolkit"
import type { PropType } from "vue"
import { computed, onUnmounted, ref, watch } from "vue"
import type { TitleRenameReferenceHandling } from "@/store/StoredApiCollection"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { normalizeNoteContent } from "@/utils/normalizeNoteContent"
import { hasNewWikiLinkTexts } from "@/utils/noteContentWikiLinks"

const storageAccessor = useStorageAccessor()

const TITLE_RENAME_REFERENCE_SAVE_OPTIONS: {
  value: TitleRenameReferenceHandling
  label: string
  testid: string
}[] = [
  {
    value: "UPDATE_VISIBLE_TEXT",
    label: "Update visible reference text",
    testid: "referenced-title-save-update-visible-text",
  },
  {
    value: "KEEP_VISIBLE_TEXT",
    label: "Keep visible reference text",
    testid: "referenced-title-save-keep-visible-text",
  },
]

const props = defineProps({
  field: {
    type: String as PropType<"edit title" | "edit content">,
    required: true,
  },
  value: {
    type: String,
    required: false,
  },
  titleRenameNeedsExplicitReferenceChoice: { type: Boolean, default: false },
  titleEditNoteId: { type: Number, required: false },
})

const savedVersion = ref(0)
const lastSavedValue = ref(props.value ?? "")
const pendingSaveValues = new Set<string>()

const needsExplicitReferencedTitleSave = (): boolean =>
  props.field === "edit title" && props.titleRenameNeedsExplicitReferenceChoice

const savingReferencedTitle = ref(false)
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
      .updateTextField(noteId, props.field, newValue)
      .catch(errorHander)
    savedVersion.value = version
    lastSavedValue.value = newValue
  } finally {
    pendingSaveValues.delete(newValue)
  }
}
const changer = debounce(changerInner, 1000)

const localValue = ref(props.value ?? "")
const version = ref(0)
const errors = ref({} as Record<string, string>)

const hasUnsavedChanges = (): boolean => {
  if (props.field === "edit content") {
    const normalizedCurrent = normalizeNoteContent(localValue.value ?? "")
    const normalizedSaved = normalizeNoteContent(lastSavedValue.value ?? "")
    return normalizedCurrent !== normalizedSaved
  }
  return localValue.value !== lastSavedValue.value
}

const discardReferencedTitleDraft = () => {
  errors.value = {}
  localValue.value = lastSavedValue.value ?? ""
  version.value = savedVersion.value
}

const scheduleReferencedTitleBlurDiscardCheck = (root: HTMLElement) => {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      if (!needsExplicitReferencedTitleSave() || !hasUnsavedChanges()) return
      const focused = document.activeElement
      if (focused instanceof Node && root.contains(focused)) return
      discardReferencedTitleDraft()
    })
  })
}

const onReferencedTitleFocusOut = (event: FocusEvent) => {
  if (!needsExplicitReferencedTitleSave() || !hasUnsavedChanges()) return
  changer.cancel()
  const root = event.currentTarget as HTMLElement
  const next = event.relatedTarget as Node | null
  if (next && root.contains(next)) return
  scheduleReferencedTitleBlurDiscardCheck(root)
}

const showReferencedTitleSavePanel = computed(
  () => needsExplicitReferencedTitleSave() && hasUnsavedChanges()
)

const wrapperClass = computed(() => {
  if (hasUnsavedChanges()) {
    return "dirty"
  }
  return ""
})

const onUpdate = (noteId: number, newValue: string) => {
  if (props.field === "edit title" && !newValue.trim()) {
    return
  }

  if (props.field === "edit content") {
    const normalizedNewValue = normalizeNoteContent(newValue)
    const normalizedLastSaved = normalizeNoteContent(lastSavedValue.value ?? "")
    const prevNormalized = normalizeNoteContent(localValue.value ?? "")

    errors.value = {}
    localValue.value = newValue

    if (normalizedNewValue === normalizedLastSaved) {
      return
    }

    changer(noteId, normalizedNewValue, version.value + 1, setError)
    version.value += 1
    if (hasNewWikiLinkTexts(prevNormalized, normalizedNewValue)) {
      changer.flush()
    }
    return
  }

  errors.value = {}
  localValue.value = newValue

  if (needsExplicitReferencedTitleSave()) {
    version.value += 1
    return
  }

  changer(noteId, newValue, version.value + 1, setError)
  version.value += 1
}

const onBlur = () => {
  if (needsExplicitReferencedTitleSave()) {
    changer.cancel()
    return
  }
  changer.flush()
}

const is401Error = (errs: unknown): boolean =>
  typeof errs === "object" &&
  errs !== null &&
  "status" in errs &&
  errs.status === 401

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

const saveReferencedTitleWithChoice = async (
  referenceHandling: TitleRenameReferenceHandling
) => {
  if (!needsExplicitReferencedTitleSave() || !hasUnsavedChanges()) return
  const noteId = props.titleEditNoteId
  if (noteId == null) return
  savingReferencedTitle.value = true
  errors.value = {}
  try {
    await storageAccessor.value
      .storedApi()
      .updateTextField(noteId, "edit title", localValue.value, {
        titleReferenceHandling: referenceHandling,
      })
    savedVersion.value = version.value
    lastSavedValue.value = localValue.value
  } catch (errs: unknown) {
    setError(errs)
  } finally {
    savingReferencedTitle.value = false
  }
}

const handleNavigation = (newValue: string) => {
  changer.cancel()
  version.value = savedVersion.value
  localValue.value = newValue
  lastSavedValue.value = newValue
}

const updateToPropValue = (newValue: string | undefined) => {
  const valueToSet = newValue ?? ""
  localValue.value = valueToSet
  lastSavedValue.value = valueToSet
}

const handlePropChange = (newValue: string | undefined) => {
  if (version.value !== savedVersion.value) {
    const normalizedNewValue = newValue ?? ""
    if (
      normalizedNewValue !== "" &&
      pendingSaveValues.has(normalizedNewValue)
    ) {
      return
    }
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

watch(() => props.value, handlePropChange)

onUnmounted(() => {
  needsExplicitReferencedTitleSave() ? changer.cancel() : changer.flush()
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
