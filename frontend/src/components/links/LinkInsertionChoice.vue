<template>
  <div class="daisy-flex daisy-flex-col daisy-gap-3">
    <div>
      Link to:
      <strong>
        <NoteTitleComponent v-bind="{ noteTopology: targetNoteTopology }" />
      </strong>
    </div>
    <div class="daisy-flex daisy-flex-col daisy-gap-2">
      <button
        v-if="deadLinkDisplayText"
        class="daisy-btn daisy-btn-primary"
        @click="$emit('chooseLinkDeadLink')"
      >
        Link "{{ deadLinkDisplayText }}" to this note
      </button>
      <button
        v-if="!deadLinkDisplayText"
        class="daisy-btn daisy-btn-primary"
        @click="$emit('chooseInsertWikiLink')"
      >
        Insert as a wiki link
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
      <Reply class="daisy-w-6 daisy-h-6" />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { NoteTopology } from "@generated/doughnut-backend-api"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import { Reply } from "lucide-vue-next"

defineProps<{
  targetNoteTopology: NoteTopology
  wikiPropertyOptionAvailable?: boolean
  deadLinkDisplayText?: string
}>()

defineEmits<{
  chooseInsertWikiLink: []
  chooseInsertWikiLinkAsProperty: []
  chooseAddRelationship: []
  chooseLinkDeadLink: []
  goBack: []
}>()
</script>
