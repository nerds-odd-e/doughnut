<template>
  <div>
    <span class="daisy-label daisy-p-0">Relation note location</span>
    <RadioButtons
      field=""
      scope-name="relationship-placement"
      v-model="formData.relationshipNotePlacement"
      :options="placementOptions"
    />
    <RelationTypeSelect
      field="relationType"
      scope-name="relationship"
      v-model="formData.relationType"
      :error-message="relationshipFormErrors.relationType"
      :inverse-icon="true"
      @update:model-value="relationTypeSelected"
    />
    <div>
      Target:
      <strong
        ><NoteTitleComponent
          v-if="targetNoteTopology"
          v-bind="{ noteTopology: targetNoteTopology }"
      /></strong>
    </div>
    <button class="daisy-btn daisy-btn-secondary go-back-button" @click="$emit('goBack')">
      <Reply class="w-5 h-5" />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { Note } from "@generated/doughnut-backend-api"
import type { RelationshipCreation } from "@generated/doughnut-backend-api"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import RadioButtons from "../form/RadioButtons.vue"
import RelationTypeSelect from "./RelationTypeSelect.vue"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import { Reply } from "lucide-vue-next"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const storageAccessor = useStorageAccessor()

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  targetNoteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
})

const emit = defineEmits(["success", "goBack"])

const placementOptions: {
  value: NonNullable<RelationshipCreation["relationshipNotePlacement"]>
  label: string
  title: string
}[] = [
  {
    value: "relations_subfolder",
    label: "“relations” subfolder",
    title:
      "Create or use a folder named relations under the folder that contains the source note.",
  },
  {
    value: "same_level_as_source",
    label: "Same level as source",
    title: "Place the relation note in the same folder as the source note.",
  },
  {
    value: "named_after_source_note",
    label: "Folder named like source",
    title: "Create or use a subfolder with the same name as the source note.",
  },
]

const formData = ref<Partial<RelationshipCreation>>({
  relationType: undefined,
  relationshipNotePlacement: "relations_subfolder",
})

const relationshipFormErrors = ref({
  relationType: undefined as string | undefined,
})

const relationTypeSelected = async (
  relationType: RelationshipCreation["relationType"]
) => {
  try {
    if (relationType !== undefined) {
      await storageAccessor.value
        .storedApi()
        .createRelationship(props.note.id, props.targetNoteTopology.id, {
          relationType: relationType,
          relationshipNotePlacement: formData.value.relationshipNotePlacement,
        })
    }

    emit("success")
  } catch (res) {
    relationshipFormErrors.value = res as {
      asFirstChild: string | undefined
      relationType: string | undefined
      moveUnder: string | undefined
    }
  }
}
</script>
