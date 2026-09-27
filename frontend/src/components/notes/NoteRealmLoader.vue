<template>
  <slot :note-realm="noteRealm" />
</template>

<script setup lang="ts">
import { computed, ref, toRefs, watch } from "vue"
import { useNoteStore } from "@/store/noteStore"

const props = defineProps({
  noteId: { type: Number, required: true },
})

const noteStore = useNoteStore()
const reactiveProps = toRefs(props)

const loadGeneration = ref(0)

watch(
  () => reactiveProps.noteId.value,
  async (noteId) => {
    const my = ++loadGeneration.value
    await noteStore.loadNoteRealm(noteId)
    if (my !== loadGeneration.value) return
  },
  { immediate: true }
)

const noteRealmRef = computed(() =>
  noteStore.refOfNoteRealm(reactiveProps.noteId.value)
)

const noteRealm = computed(() => noteRealmRef.value?.value)
</script>
