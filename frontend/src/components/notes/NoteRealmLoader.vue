<template>
  <slot :note-realm="noteRealm" />
</template>

<script setup lang="ts">
import { computed, ref, toRefs, watch } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps({
  noteId: { type: Number, required: true },
})

const storageAccessor = useStorageAccessor()
const reactiveProps = toRefs(props)

const loadGeneration = ref(0)

watch(
  () => reactiveProps.noteId.value,
  async (noteId) => {
    const my = ++loadGeneration.value
    await storageAccessor.value.storedApi().loadNoteRealm(noteId)
    if (my !== loadGeneration.value) return
  },
  { immediate: true }
)

const noteRealmRef = computed(() =>
  storageAccessor.value.storedApi().getNoteRealmRef(reactiveProps.noteId.value)
)

const noteRealm = computed(() => noteRealmRef.value?.value)
</script>
