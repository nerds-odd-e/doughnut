<template>
  <slot :note-realm="noteRealm" />
</template>

<script setup lang="ts">
import { computed, toRefs } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps({
  noteId: { type: Number, required: true },
})

const storageAccessor = useStorageAccessor()
const reactiveProps = toRefs(props)

const noteRealmRef = computed(() =>
  storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndReloadPosition(reactiveProps.noteId.value)
)

const noteRealm = computed(() => noteRealmRef.value?.value)
</script>
