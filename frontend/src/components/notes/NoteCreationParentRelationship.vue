<template>
  <div class="mb-4" data-testid="note-creation-parent-relationship">
    <RadioButtons
      field="relationship"
      scope-name="note-creation"
      v-model="model"
      :options="options"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import RadioButtons from "@/components/form/RadioButtons.vue"
import {
  parentRelationshipOptions,
  type NoteCreationParentRelationship,
} from "@/utils/noteCreationParentRelationship"

const model = defineModel<NoteCreationParentRelationship>({ required: true })

const props = defineProps<{
  contextContent?: string
}>()

const options = computed(() => parentRelationshipOptions(props.contextContent))
</script>
