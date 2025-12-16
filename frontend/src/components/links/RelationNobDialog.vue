<template>
  <h3>Relationship</h3>
  <RelationTypeSelect
    field="relationType"
    scope-name="relationship"
    v-model="formData.relationType"
    :error-message="relationshipFormErrors.relationType"
    :inverse-icon="true"
    @update:model-value="updateRelationship"
  />
  <div>
    Target:
    <strong>
      <NoteTitleWithLink
        v-if="noteTopology.targetNoteTopology"
        class="relationship-title"
        v-bind="{ noteTopology: noteTopology.targetNoteTopology }"
      />
    </strong>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { RelationshipCreation, NoteTopology } from "@generated/backend"
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
const formData = ref<RelationshipCreation>({
  relationType: props.noteTopology.relationType!,
})

const relationshipFormErrors = ref<{ relationType?: string }>({
  relationType: undefined,
})

// Methods
const updateRelationship = () => {
  storageAccessor.value
    .storedApi()
    .updateRelationship(props.noteTopology.id, formData.value)
    .then(() => emit("closeDialog"))
    .catch((error) => {
      relationshipFormErrors.value = error
    })
}
</script>

<style scoped>
.relation-nob {
  padding: 3px;
}
</style>
