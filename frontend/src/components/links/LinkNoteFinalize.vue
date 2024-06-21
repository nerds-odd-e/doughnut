<template>
  <div>
    <LinkTypeSelect
      field="linkType"
      scope-name="link"
      v-model="formData.linkType"
      :errors="linkFormErrors.linkType"
      :inverse-icon="true"
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
      :errors="linkFormErrors.moveUnder"
      field="alsoMoveToUnderTargetNote"
    />

    <RadioButtons
      v-if="!!formData.moveUnder"
      scope-name="link"
      v-model="formData.asFirstChild"
      :errors="linkFormErrors.asFirstChild"
      :options="[
        { value: true, label: 'as its first child' },
        { value: false, label: 'as its last child' },
      ]"
    />

    <button class="btn btn-secondary go-back-button" @click="$emit('goBack')">
      <SvgGoBack />
    </button>
    <button class="btn btn-primary" @click.once="createLink()">
      Create Link
    </button>
  </div>
</template>

<script setup lang="ts">
import { LinkCreation, Note, NoteTopic } from "@/generated/backend"
import { PropType, ref } from "vue"
import { StorageAccessor } from "../../store/createNoteStorage"
import usePopups from "../commons/Popups/usePopups"

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

const createLink = async () => {
  if (formData.value.moveUnder && props.note.parentId === null) {
    if (
      !(await popups.confirm(
        `"${props.note.noteTopic.topicConstructor}" is a top level notebook. Do you want to move it under other notebook?`,
      ))
    ) {
      return
    }
  }
  props.storageAccessor
    .storedApi()
    .createLink(props.note.id, props.targetNoteTopic.id, formData.value)
    .then((r) => emit("success", r))
    .catch((res) => {
      linkFormErrors.value = res
    })
}
</script>
