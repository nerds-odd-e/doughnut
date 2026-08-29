<template>
  <DropdownMenuItem v-if="show('new')">
    <DropdownMenuActionButton
      :title="titles.new"
      :icon="NotebookPen"
      @click="onNew"
    />
  </DropdownMenuItem>

  <DropdownMenuItem v-if="show('wiki')">
    <DropdownMenuActionButton
      :title="titles.wiki"
      :icon="Search"
      @click="onWiki"
    />
  </DropdownMenuItem>

  <DropdownMenuItem v-if="show('conversation')">
    <DropdownMenuActionButton
      :title="titles.conversation"
      :icon="MessageCircle"
      @click="onConversation"
    />
  </DropdownMenuItem>

  <DropdownMenuItem v-if="show('edit')">
    <DropdownMenuActionButton
      :title="editTitle"
      :icon="asMarkdown ? LayoutTemplate : FileCode"
      @click="onEdit"
    />
  </DropdownMenuItem>
</template>

<script setup lang="ts">
import {
  FileCode,
  LayoutTemplate,
  MessageCircle,
  NotebookPen,
  Search,
} from "@lucide/vue"
import DropdownMenuActionButton from "@/components/commons/DropdownMenuActionButton.vue"
import DropdownMenuItem from "@/components/commons/DropdownMenuItem.vue"
import { currentRouteSettingConversation } from "@/routes/noteShowLocation"
import { computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  noteMoreOptionsTitles,
  noteToolbarEditTitle,
  type NoteMoreOptionsActionId,
} from "./noteMoreOptionsTitles"

const titles = noteMoreOptionsTitles

const props = withDefaults(
  defineProps<{
    only?: NoteMoreOptionsActionId[]
    asMarkdown?: boolean
  }>(),
  { asMarkdown: false }
)

const emit = defineEmits<{
  (e: "close-dialog"): void
  (e: "edit-as-markdown", value: boolean): void
  (e: "open-wiki"): void
  (e: "open-new"): void
}>()

const router = useRouter()
const route = useRoute()
const editTitle = computed(() => noteToolbarEditTitle(props.asMarkdown))
const show = (id: NoteMoreOptionsActionId) =>
  !props.only || props.only.includes(id)

const closeAfter = (action: () => void) => {
  action()
  emit("close-dialog")
}

const onNew = () => closeAfter(() => emit("open-new"))
const onWiki = () => closeAfter(() => emit("open-wiki"))
const onConversation = () =>
  closeAfter(() => router.replace(currentRouteSettingConversation(route, true)))
const onEdit = () =>
  closeAfter(() => emit("edit-as-markdown", !props.asMarkdown))
</script>
