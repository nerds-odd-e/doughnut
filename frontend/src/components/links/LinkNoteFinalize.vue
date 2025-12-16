<template>
  <div>
    <RelationTypeSelect
      field="relationType"
      scope-name="link"
      v-model="formData.relationType"
      :error-message="linkFormErrors.relationType"
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
      <SvgGoBack />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref } from "vue"
import type { Note } from "@generated/backend"
import type { LinkCreation } from "@generated/backend"
import type { NoteTopology } from "@generated/backend"
import RelationTypeSelect from "./RelationTypeSelect.vue"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import SvgGoBack from "../svgs/SvgGoBack.vue"
import usePopups from "../commons/Popups/usePopups"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const { popups } = usePopups()
const storageAccessor = useStorageAccessor()

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  targetNoteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
})

const emit = defineEmits(["success", "goBack"])

const formData = ref<LinkCreation>({
  relationType: "no link",
})

const linkFormErrors = ref({
  relationType: undefined as string | undefined,
})

const relationTypeSelected = async (
  relationType: LinkCreation["relationType"]
) => {
  if (props.note.parentId === null) {
    if (
      !(await popups.confirm(
        `"${props.note.noteTopology.titleOrPredicate}" is a top level notebook. Do you want to move it under other notebook?`
      ))
    ) {
      return
    }
  }

  try {
    if (relationType !== "no link") {
      await storageAccessor.value
        .storedApi()
        .createLink(props.note.id, props.targetNoteTopology.id, {
          relationType: formData.value.relationType,
        })
    }

    emit("success")
  } catch (res) {
    linkFormErrors.value = res as {
      asFirstChild: string | undefined
      relationType: string | undefined
      moveUnder: string | undefined
    }
  }
}
</script>
