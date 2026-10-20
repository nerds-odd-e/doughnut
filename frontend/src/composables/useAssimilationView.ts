import {
  computed,
  onScopeDispose,
  ref,
  shallowReactive,
  toValue,
  type MaybeRefOrGetter,
} from "vue"

const targetNoteId = ref<number | null>(null)
const showAssimilationPanel = ref(false)

const presentations = shallowReactive(
  new Set<MaybeRefOrGetter<number | undefined>>()
)

export function useAssimilationView() {
  const trackPresentation = (
    noteId: MaybeRefOrGetter<number | undefined>,
    visible: MaybeRefOrGetter<boolean> = true
  ) => {
    const presentedNote = computed(() =>
      toValue(visible) ? toValue(noteId) : undefined
    )
    presentations.add(presentedNote)
    onScopeDispose(() => presentations.delete(presentedNote))
  }

  const isPresentedForNote = (noteId: number) =>
    [...presentations].some((presentation) => toValue(presentation) === noteId)

  const isOpenForNote = (noteId: number) =>
    showAssimilationPanel.value && targetNoteId.value === noteId

  const openForNote = (noteId: number) => {
    targetNoteId.value = noteId
    showAssimilationPanel.value = true
  }

  const resetForNote = (noteId: number) => {
    showAssimilationPanel.value = targetNoteId.value === noteId
  }

  const dismiss = () => {
    targetNoteId.value = null
    showAssimilationPanel.value = false
  }

  const toggle = (noteId: number) => {
    if (isOpenForNote(noteId)) {
      dismiss()
      return
    }
    openForNote(noteId)
  }

  return {
    trackPresentation,
    isPresentedForNote,
    showAssimilationPanel,
    targetNoteId,
    isOpenForNote,
    openForNote,
    resetForNote,
    dismiss,
    toggle,
  }
}
