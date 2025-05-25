<template>
  <h3>Link</h3>
  <LinkTypeSelect
    field="linkType"
    scope-name="link"
    v-model="formData.linkType"
    :error-message="linkFormErrors.linkType"
    :inverse-icon="true"
    @update:model-value="updateLink"
  />
  <div>
    Target:
    <strong>
      <NoteTitleWithLink
        v-if="noteTopology.objectNoteTopology"
        class="link-title"
        v-bind="{ noteTopology: noteTopology.objectNoteTopology }"
      />
    </strong>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { LinkCreation, NoteTopology } from "@/generated/backend"
import type { StorageAccessor } from "../../store/createNoteStorage"
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
import LinkTypeSelect from "./LinkTypeSelect.vue"

// Props definition
const props = defineProps<{
  noteTopology: NoteTopology
  storageAccessor: StorageAccessor
}>()

// Emits definition
const emit = defineEmits<{
  (e: "closeDialog"): void
}>()

// Reactive state
const formData = ref<LinkCreation>({
  linkType: props.noteTopology.linkType!,
})

const linkFormErrors = ref<{ linkType?: string }>({ linkType: undefined })

// Methods
const updateLink = () => {
  props.storageAccessor
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
