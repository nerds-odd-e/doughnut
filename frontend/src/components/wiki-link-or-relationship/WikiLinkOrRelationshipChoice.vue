<template>
  <div class="flex flex-col gap-3">
    <div>
      Target:
      <strong>
        <NoteTitleComponent v-bind="{ noteTopology: targetNoteTopology }" />
      </strong>
    </div>
    <div class="flex flex-col gap-2">
      <button
        v-if="showBareWikiPrimary"
        class="daisy-btn daisy-btn-primary"
        @click="onPrimaryClick"
      >
        {{ primaryLabel }}
      </button>
      <button
        v-if="wikiPropertyOptionAvailable && !deadWikiLinkDisplayText"
        class="daisy-btn daisy-btn-accent"
        @click="$emit('chooseInsertWikiLinkAsProperty')"
      >
        Add wiki link as a new property
      </button>
      <button
        v-if="relationshipOptionAvailable && !deadWikiLinkDisplayText"
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
import { Reply } from "@lucide/vue"

const props = withDefaults(
  defineProps<{
    targetNoteTopology: NoteTopology
    wikiPropertyOptionAvailable?: boolean
    deadWikiLinkDisplayText?: string
    bareWikiLinkAvailable?: boolean
    relationshipOptionAvailable?: boolean
  }>(),
  {
    bareWikiLinkAvailable: true,
    relationshipOptionAvailable: true,
  }
)

const emit = defineEmits<{
  chooseInsertWikiLink: []
  chooseInsertWikiLinkAsProperty: []
  chooseAddRelationship: []
  chooseDeadWikiLink: []
  goBack: []
}>()

const primaryLabel = computed(() =>
  props.deadWikiLinkDisplayText
    ? `Point wiki link "${props.deadWikiLinkDisplayText}" at this note`
    : "Insert as a wiki link"
)

const showBareWikiPrimary = computed(
  () => props.bareWikiLinkAvailable || Boolean(props.deadWikiLinkDisplayText)
)

function onPrimaryClick() {
  if (props.deadWikiLinkDisplayText) emit("chooseDeadWikiLink")
  else emit("chooseInsertWikiLink")
}
</script>
