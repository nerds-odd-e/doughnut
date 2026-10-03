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
import type { PropType } from "vue"
import { computed, onUnmounted, ref, toRef, watch } from "vue"
import type { TitleRenameReferenceHandling } from "@/store/noteStore"
import { useNoteStore } from "@/store/noteStore"
import { useDebouncedTextAutosave } from "@/composables/useDebouncedTextAutosave"
import { normalizeNoteContent } from "@/utils/normalizeNoteContent"
import { hasNewWikiLinkTexts } from "@/utils/noteContentWikiLinks"
import {
  noteContentMutationAdmissionIsOpen,
  registerNoteContentAutosave,
} from "@/composables/noteContentMutationBarrier"

const noteStore = useNoteStore()

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
  noteId: { type: Number, required: true },
  beforeSaveContent: {
    type: Function as PropType<
      (lastSaved: string, newValue: string) => Promise<boolean>
    >,
    required: false,
  },
})

const needsExplicitReferencedTitleSave = (): boolean =>
  props.field === "edit title" && props.titleRenameNeedsExplicitReferenceChoice

const savingReferencedTitle = ref(false)
const activeNoteId = ref<number | null>(null)

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

const autosave = useDebouncedTextAutosave({
  externalValue: toRef(props, "value"),
  persist: async (value) => {
    const noteId = activeNoteId.value
    if (noteId == null) return
    await noteStore.updateTextField(noteId, props.field, value)
  },
  normalize:
    props.field === "edit content" ? normalizeNoteContent : (value) => value,
  beforePersist: async (lastSaved, newValue) => {
    if (props.field !== "edit content" || !props.beforeSaveContent) {
      return true
    }
    return props.beforeSaveContent(lastSaved, newValue)
  },
  shouldFlushImmediately:
    props.field === "edit content"
      ? (prev, next) => hasNewWikiLinkTexts(prev, next)
      : undefined,
  onError: setError,
  cancelOnUnmount: needsExplicitReferencedTitleSave(),
})

const {
  localValue,
  errors,
  isDirty,
  hasUnsavedChanges,
  propose,
  flush,
  flushAndWait,
  cancel,
  discardDraft,
  markSaved,
} = autosave

let unregisterContentAutosave: (() => void) | undefined

if (props.field === "edit content") {
  watch(
    () => props.noteId,
    (noteId) => {
      unregisterContentAutosave?.()
      unregisterContentAutosave = registerNoteContentAutosave(noteId, {
        flushAndWait,
      })
    },
    { immediate: true }
  )
  onUnmounted(() => unregisterContentAutosave?.())
}

const showReferencedTitleSavePanel = computed(
  () => needsExplicitReferencedTitleSave() && hasUnsavedChanges()
)

const wrapperClass = computed(() => (isDirty.value ? "dirty" : ""))

const onUpdate = (newValue: string) => {
  if (props.field === "edit title" && !newValue.trim()) {
    return
  }

  if (
    props.field === "edit content" &&
    !noteContentMutationAdmissionIsOpen(props.noteId)
  ) {
    return
  }

  activeNoteId.value = props.noteId

  if (needsExplicitReferencedTitleSave()) {
    errors.value = {}
    localValue.value = newValue
    return
  }

  propose(newValue)
}

const onBlur = () => {
  if (needsExplicitReferencedTitleSave()) {
    cancel()
    return
  }
  flush()
}

const scheduleReferencedTitleBlurDiscardCheck = (root: HTMLElement) => {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      if (!needsExplicitReferencedTitleSave() || !hasUnsavedChanges()) return
      const focused = document.activeElement
      if (focused instanceof Node && root.contains(focused)) return
      discardDraft()
    })
  })
}

const onReferencedTitleFocusOut = (event: FocusEvent) => {
  if (!needsExplicitReferencedTitleSave() || !hasUnsavedChanges()) return
  cancel()
  const root = event.currentTarget as HTMLElement
  const next = event.relatedTarget as Node | null
  if (next && root.contains(next)) return
  scheduleReferencedTitleBlurDiscardCheck(root)
}

const saveReferencedTitleWithChoice = async (
  referenceHandling: TitleRenameReferenceHandling
) => {
  if (!needsExplicitReferencedTitleSave() || !hasUnsavedChanges()) return
  savingReferencedTitle.value = true
  errors.value = {}
  try {
    await noteStore.updateTextField(
      props.noteId,
      "edit title",
      localValue.value,
      {
        titleReferenceHandling: referenceHandling,
      }
    )
    markSaved(localValue.value)
  } catch (errs: unknown) {
    setError(errs)
  } finally {
    savingReferencedTitle.value = false
  }
}
</script>
