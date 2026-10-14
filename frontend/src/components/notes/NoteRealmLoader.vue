<template>
  <slot :note-realm="noteRealm" :fresh="loadState === 'loaded'" :loading="loadState === 'loading'" :load-failed="loadState === 'failed'" :reload="load" />
</template>

<script setup lang="ts">
import { computed, ref, toRefs, watch } from "vue"
import type { NoteRealm } from "@generated/donut-backend-api"
import { useNoteStore } from "@/store/noteStore"

const props = defineProps({
  noteId: { type: Number, required: true },
  recoverLoadFailure: { type: Boolean, default: false },
})
const emit = defineEmits<{ (e: "loaded", noteRealm: NoteRealm): void }>()

const noteStore = useNoteStore()
const reactiveProps = toRefs(props)

const loadGeneration = ref(0)
const loadState = ref<"loading" | "loaded" | "failed">("loading")

const load = async () => {
  const my = ++loadGeneration.value
  loadState.value = "loading"
  try {
    const loaded = await noteStore.loadNoteRealm(props.noteId)
    if (my === loadGeneration.value) {
      emit("loaded", loaded)
      loadState.value = "loaded"
    }
  } catch (error) {
    if (my !== loadGeneration.value) return
    loadState.value = "failed"
    if (!props.recoverLoadFailure) throw error
  }
}

watch(() => reactiveProps.noteId.value, load, { immediate: true })

const noteRealmRef = computed(() =>
  noteStore.refOfNoteRealm(reactiveProps.noteId.value)
)

const noteRealm = computed(() => noteRealmRef.value?.value)
</script>
