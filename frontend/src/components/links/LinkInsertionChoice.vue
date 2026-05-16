<template>
  <div class="flex flex-col gap-3">
    <div>
      Link to:
      <strong>
        <NoteTitleComponent v-bind="{ noteTopology: targetNoteTopology }" />
      </strong>
    </div>
    <div class="flex flex-col gap-2">
      <button class="daisy-btn daisy-btn-primary" @click="onPrimaryClick">
        {{ primaryLabel }}
      </button>
      <button
        v-if="wikiPropertyOptionAvailable && !deadLinkDisplayText"
        class="daisy-btn daisy-btn-accent"
        @click="$emit('chooseInsertWikiLinkAsProperty')"
      >
        Add wiki link as a new property
      </button>
      <button
        v-if="!deadLinkDisplayText"
        class="daisy-btn daisy-btn-secondary"
        @click="$emit('chooseAddRelationship')"
      >
        Add a new relationship note
      </button>
    </div>
    <button class="daisy-btn daisy-btn-ghost go-back-button" @click="$emit('goBack')">
      <Reply class="w-6 h-6" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import { Reply } from "lucide-vue-next"

const props = defineProps<{
  targetNoteTopology: NoteTopology
  wikiPropertyOptionAvailable?: boolean
  deadLinkDisplayText?: string
}>()

const emit = defineEmits<{
  chooseInsertWikiLink: []
  chooseInsertWikiLinkAsProperty: []
  chooseAddRelationship: []
  chooseLinkDeadLink: []
  goBack: []
}>()

const primaryLabel = computed(() =>
  props.deadLinkDisplayText
    ? `Link "${props.deadLinkDisplayText}" to this note`
    : "Insert as a wiki link"
)

function onPrimaryClick() {
  if (props.deadLinkDisplayText) emit("chooseLinkDeadLink")
  else emit("chooseInsertWikiLink")
}
</script>
