<template>
  <div>
    <LinkTypeSelect
      field="linkType"
      scope-name="link"
      v-model="formData.linkType"
      :error-message="linkFormErrors.linkType"
      :inverse-icon="true"
      @update:model-value="linkTypeSelected"
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
import { NoteTopology as NoteTopologyConstants } from "@/managedApi/linkTypeConstants"
import LinkTypeSelect from "./LinkTypeSelect.vue"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import SvgGoBack from "../svgs/SvgGoBack.vue"
import usePopups from "../commons/Popups/usePopups"
import type { StorageAccessor } from "../../store/createNoteStorage"

const { popups } = usePopups()

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  targetNoteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const emit = defineEmits(["success", "goBack"])

const formData = ref<LinkCreation>({
  linkType: NoteTopologyConstants.linkType.NO_LINK,
})

const linkFormErrors = ref({
  linkType: undefined as string | undefined,
})

const linkTypeSelected = async (linkType: LinkCreation["linkType"]) => {
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
    if (linkType !== NoteTopologyConstants.linkType.NO_LINK) {
      await props.storageAccessor
        .storedApi()
        .createLink(props.note.id, props.targetNoteTopology.id, {
          linkType: formData.value.linkType,
        })
    }

    emit("success")
  } catch (res) {
    linkFormErrors.value = res as {
      asFirstChild: string | undefined
      linkType: string | undefined
      moveUnder: string | undefined
    }
  }
}
</script>
