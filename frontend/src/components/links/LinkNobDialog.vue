<template>
  <h3>Link</h3>
  <RelationTypeSelect
    field="relationType"
    scope-name="link"
    v-model="formData.relationType"
    :error-message="linkFormErrors.relationType"
    :inverse-icon="true"
    @update:model-value="updateLink"
  />
  <div>
    Target:
    <strong>
      <NoteTitleWithLink
        v-if="noteTopology.targetNoteTopology"
        class="link-title"
        v-bind="{ noteTopology: noteTopology.targetNoteTopology }"
      />
    </strong>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { LinkCreation, NoteTopology } from "@generated/backend"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
import RelationTypeSelect from "./RelationTypeSelect.vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()

// Props definition
const props = defineProps<{
  noteTopology: NoteTopology
}>()

// Emits definition
const emit = defineEmits<{
  (e: "closeDialog"): void
}>()

// Reactive state
const formData = ref<LinkCreation>({
  relationType: props.noteTopology.relationType!,
})

const linkFormErrors = ref<{ relationType?: string }>({
  relationType: undefined,
})

// Methods
const updateLink = () => {
  storageAccessor.value
    .storedApi()
    .updateLink(props.noteTopology.id, formData.value)
    .then(() => emit("closeDialog"))
    .catch((error) => {
      linkFormErrors.value = error
    })
}
</script>

<style scoped>
.link-nob {
  padding: 3px;
}
</style>
