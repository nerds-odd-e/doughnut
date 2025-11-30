<template>
  <button title="Delete note" @click="deleteNote">
    <SvgRemove />Delete note
  </button>
</template>

<script setup lang="ts">
import { useRouter } from "vue-router"
import usePopups from "../../commons/Popups/usePopups"
import SvgRemove from "../../svgs/SvgRemove.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const router = useRouter()
const { popups } = usePopups()
const storageAccessor = useStorageAccessor()

const props = defineProps({
  noteId: { type: Number, required: true },
})

const deleteNote = async () => {
  if (await popups.confirm(`Confirm to delete this note?`)) {
    await storageAccessor.value.storedApi().deleteNote(router, props.noteId)
  }
}
</script>
