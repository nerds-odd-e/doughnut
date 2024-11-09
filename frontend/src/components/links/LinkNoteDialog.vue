<template>
  <h3 v-if="note">Search for a target note</h3>
  <h3 v-else>Searching</h3>
  <SearchNote
    v-if="!targetNoteTopic"
    v-bind="{ noteId: note?.id }"
    @selected="targetNoteTopic = $event"
    @moveUnder="moveUnder($event)"
  />
  <LinkNoteFinalize
    v-if="targetNoteTopic && note"
    v-bind="{ targetNoteTopic, note, storageAccessor }"
    @success="$emit('closeDialog')"
    @go-back="targetNoteTopic = undefined"
  />
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { Note } from "@/generated/backend"
import { NoteTopic } from "@/generated/backend"
import LinkNoteFinalize from "./LinkNoteFinalize.vue"
import SearchNote from "../search/SearchNote.vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import usePopups from "../commons/Popups/usePopups"

const { popups } = usePopups()

const { note, storageAccessor } = defineProps<{
  note?: Note
  storageAccessor: StorageAccessor
}>()

const emit = defineEmits<{
  closeDialog: []
}>()

const targetNoteTopic = ref<NoteTopic | undefined>(undefined)

async function moveUnder(targetNoteTopic: NoteTopic) {
  if (!(await popups.confirm("Move note under target note?"))) {
    return
  }
  storageAccessor
    .storedApi()
    .moveNote(note!.id, targetNoteTopic.id, {
      linkType: NoteTopic.linkType.NO_LINK,
      moveUnder: true,
      asFirstChild: false,
    })
    .then(() => {
      emit("closeDialog")
    })
}
</script>
