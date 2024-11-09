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
        ><NoteTopicComponent
          v-if="targetNoteTopic"
          v-bind="{ noteTopic: targetNoteTopic }"
      /></strong>
    </div>
    <CheckInput
      scope-name="link"
      v-model="formData.moveUnder"
      :error-message="linkFormErrors.moveUnder"
      field="alsoMoveToUnderTargetNote"
    />
    <RadioButtons
      v-if="formData.moveUnder"
      scope-name="link"
      v-model="asFirstChildModel"
      :error-message="linkFormErrors.asFirstChild"
      :options="[
        { value: 'true', label: 'as its first child' },
        { value: 'false', label: 'as its last child' },
      ]"
    />
    <button class="btn btn-secondary go-back-button" @click="$emit('goBack')">
      <SvgGoBack />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { ref, computed } from "vue"
import type { Note } from "@/generated/backend"
import { LinkCreation, NoteTopic } from "@/generated/backend"
import LinkTypeSelect from "./LinkTypeSelect.vue"
import NoteTopicComponent from "../notes/core/NoteTopicComponent.vue"
import CheckInput from "../form/CheckInput.vue"
import RadioButtons from "../form/RadioButtons.vue"
import SvgGoBack from "../svgs/SvgGoBack.vue"
import usePopups from "../commons/Popups/usePopups"
import type { StorageAccessor } from "../../store/createNoteStorage"

const { popups } = usePopups()

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
  targetNoteTopic: { type: Object as PropType<NoteTopic>, required: true },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})

const emit = defineEmits(["success", "goBack"])

const formData = ref<LinkCreation>({
  asFirstChild: false,
  moveUnder: false,
  linkType: NoteTopic.linkType.NO_LINK,
})

const linkFormErrors = ref({
  asFirstChild: undefined as string | undefined,
  linkType: undefined as string | undefined,
  moveUnder: undefined as string | undefined,
})

// Computed property to handle the string <-> boolean conversion for asFirstChild
const asFirstChildModel = computed({
  get: () => (formData.value.asFirstChild ?? false).toString(),
  set: (value: string) => {
    formData.value.asFirstChild = value === "true"
  },
})

const linkTypeSelected = async (linkType: LinkCreation["linkType"]) => {
  if (formData.value.moveUnder && props.note.parentId === null) {
    if (
      !(await popups.confirm(
        `"${props.note.noteTopic.topicConstructor}" is a top level notebook. Do you want to move it under other notebook?`
      ))
    ) {
      return
    }
  }

  try {
    // First create the link
    if (linkType !== NoteTopic.linkType.NO_LINK) {
      await props.storageAccessor
        .storedApi()
        .createLink(props.note.id, props.targetNoteTopic.id, {
          linkType: formData.value.linkType,
          moveUnder: false,
          asFirstChild: false,
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
