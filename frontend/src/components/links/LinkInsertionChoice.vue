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
        class="daisy-btn daisy-btn-primary"
        @click="$emit('chooseInsertWikiLink')"
      >
        Insert as a wiki link
      </button>
      <button
        class="daisy-btn daisy-btn-secondary"
        @click="$emit('chooseAddRelationship')"
      >
        Add a new relationship note
      </button>
    </div>
    <button class="daisy-btn daisy-btn-ghost go-back-button" @click="$emit('goBack')">
      <Reply class="w-5 h-5" />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { NoteTopology } from "@generated/doughnut-backend-api"
import NoteTitleComponent from "../notes/core/NoteTitleComponent.vue"
import { Reply } from "lucide-vue-next"

defineProps<{
  targetNoteTopology: NoteTopology
}>()

defineEmits<{
  chooseInsertWikiLink: []
  chooseAddRelationship: []
  goBack: []
}>()
</script>
