<template>
  <NoteMoreOptionsActions
    layout="toolbar"
    :omit="overflowedIds"
    v-bind="{ note }"
  />
  <AutoCollapseDropdown
    v-if="overflowedIds.length > 0"
    v-slot="{ closeDropdown, open }"
    ref="overflowDropdownRef"
    class="daisy-dropdown daisy-dropdown-end daisy-dropdown-bottom"
  >
    <summary
      :class="[
        'daisy-btn daisy-btn-ghost daisy-btn-sm list-none cursor-pointer',
        { 'daisy-btn-active': open },
      ]"
      title="more options"
      aria-label="more options"
    >
      <MoreHorizontal class="w-6 h-6" />
    </summary>
    <NoteMoreOptionsForm
      v-bind="{ note }"
      :only="overflowedIds"
      :as-markdown="asMarkdown"
      @close-dialog="closeDropdown"
      @edit-as-markdown="emit('edit-as-markdown', $event)"
      @open-wiki="emit('open-wiki')"
      @open-new="emit('open-new')"
    />
  </AutoCollapseDropdown>
</template>

<script setup lang="ts">
import type { Note } from "@generated/doughnut-backend-api"
import { MoreHorizontal } from "@lucide/vue"
import { computed, nextTick, ref, watch } from "vue"
import AutoCollapseDropdown from "@/components/commons/AutoCollapseDropdown.vue"
import {
  computeNoteToolbarOverflow,
  NOTE_TOOLBAR_MORE_OPTIONS_ORDER,
} from "@/composables/noteToolbarOverflow"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useNoteToolbarPanel } from "@/composables/useNoteToolbarPanel"
import NoteMoreOptionsActions from "./NoteMoreOptionsActions.vue"
import NoteMoreOptionsForm from "./NoteMoreOptionsForm.vue"
import {
  noteMoreOptionsTitles,
  noteToolbarOverflowTitles,
  type NoteMoreOptionsActionId,
} from "./noteMoreOptionsTitles"

const props = defineProps<{
  note: Note
  toolbarNav: HTMLElement | null
  asMarkdown?: boolean
  hasNewNote?: boolean
  hasConversation?: boolean
}>()

const emit = defineEmits<{
  (e: "overflowed-ids", ids: NoteMoreOptionsActionId[]): void
  (e: "edit-as-markdown", value: boolean): void
  (e: "open-wiki"): void
  (e: "open-new"): void
}>()

const overflowDropdownRef = ref<InstanceType<
  typeof AutoCollapseDropdown
> | null>(null)

const overflowedIds = ref<NoteMoreOptionsActionId[]>([])
const cachedWidths: Partial<Record<NoteMoreOptionsActionId, number>> = {}
let cachedOverflowButtonWidth = 0

const { isAudioOpen } = useNoteToolbarPanel()
const { isOpenForNote } = useAssimilationView()

const pinnedIds = computed(() => {
  const ids: NoteMoreOptionsActionId[] = []
  if (isAudioOpen.value) ids.push("audio")
  if (isOpenForNote(props.note.id)) ids.push("assimilation")
  return ids
})

const closeOverflowMenu = () => {
  overflowDropdownRef.value?.closeDropdown()
}

defineExpose({ closeOverflowMenu })

const controlByTitle = (root: ParentNode, title: string) => {
  for (const el of root.querySelectorAll("[title]")) {
    if (el.getAttribute("title") === title) return el as HTMLElement
  }
  return null
}

const sameIds = (
  left: readonly NoteMoreOptionsActionId[],
  right: readonly NoteMoreOptionsActionId[]
) =>
  left.length === right.length && left.every((id, index) => id === right[index])

const measureAndCompute = () => {
  const nav = props.toolbarNav
  if (!nav) return
  const group = nav.querySelector(".daisy-btn-group")
  if (!(group instanceof HTMLElement)) return

  for (const id of NOTE_TOOLBAR_MORE_OPTIONS_ORDER) {
    const el = noteToolbarOverflowTitles(id)
      .map((title) => controlByTitle(group, title))
      .find((found) => found !== null)
    const width = el?.offsetWidth ?? 0
    if (width > 0) cachedWidths[id] = width
  }

  const overflowEl = controlByTitle(group, noteMoreOptionsTitles.overflowMenu)
  const overflowWidth = overflowEl?.offsetWidth ?? 0
  if (overflowWidth > 0) cachedOverflowButtonWidth = overflowWidth

  const nextOverflowedIds = computeNoteToolbarOverflow({
    presentIds: NOTE_TOOLBAR_MORE_OPTIONS_ORDER.filter((id) => {
      if (id === "new") return props.hasNewNote === true
      if (id === "conversation") return props.hasConversation !== false
      return true
    }),
    pinnedIds: pinnedIds.value,
    widthById: cachedWidths,
    overflowButtonWidth: cachedOverflowButtonWidth,
    availableWidth: nav.clientWidth,
  })
  if (!sameIds(overflowedIds.value, nextOverflowedIds)) {
    overflowedIds.value = nextOverflowedIds
    emit("overflowed-ids", nextOverflowedIds)
  }
}

let updateQueued = false
const scheduleUpdate = () => {
  if (updateQueued) return
  updateQueued = true
  nextTick(() => {
    updateQueued = false
    measureAndCompute()
    nextTick(measureAndCompute)
  })
}

watch(
  () => props.toolbarNav,
  (el, _, onCleanup) => {
    if (!el) return
    scheduleUpdate()
    if (typeof ResizeObserver === "undefined") return
    const resizeObserver = new ResizeObserver(scheduleUpdate)
    resizeObserver.observe(el)
    onCleanup(() => resizeObserver.disconnect())
  },
  { immediate: true }
)

watch(pinnedIds, scheduleUpdate)
watch(() => [props.hasNewNote, props.hasConversation], scheduleUpdate)
</script>
