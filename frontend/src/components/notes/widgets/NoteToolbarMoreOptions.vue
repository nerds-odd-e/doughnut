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
}>()

const emit = defineEmits<{
  (e: "overflowed-ids", ids: NoteMoreOptionsActionId[]): void
  (e: "edit-as-markdown", value: boolean): void
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

const moreOptionTitles = new Set<string>([
  noteMoreOptionsTitles.overflowMenu,
  ...NOTE_TOOLBAR_MORE_OPTIONS_ORDER.flatMap((id) =>
    noteToolbarOverflowTitles(id)
  ),
])

const closeOverflowMenu = () => {
  overflowDropdownRef.value?.closeDropdown()
}

defineExpose({ closeOverflowMenu })

const layoutWidth = (el: HTMLElement): number => {
  if (getComputedStyle(el).display === "contents") {
    let sum = 0
    for (const child of el.children) {
      sum += layoutWidth(child as HTMLElement)
    }
    return sum
  }
  return el.offsetWidth
}

const isMoreOptionsControl = (el: HTMLElement) => {
  const title = el.getAttribute("title")
  if (title && moreOptionTitles.has(title)) return true
  for (const labeled of el.querySelectorAll("[title]")) {
    const labeledTitle = labeled.getAttribute("title")
    if (labeledTitle && moreOptionTitles.has(labeledTitle)) return true
  }
  return false
}

const precedingSiblingWidth = (group: HTMLElement) => {
  let sum = 0
  for (const child of group.children) {
    const el = child as HTMLElement
    if (isMoreOptionsControl(el)) break
    sum += layoutWidth(el)
  }
  return sum
}

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
    presentIds: NOTE_TOOLBAR_MORE_OPTIONS_ORDER,
    pinnedIds: pinnedIds.value,
    widthById: cachedWidths,
    overflowButtonWidth: cachedOverflowButtonWidth,
    availableWidth: nav.clientWidth - precedingSiblingWidth(group),
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
</script>
