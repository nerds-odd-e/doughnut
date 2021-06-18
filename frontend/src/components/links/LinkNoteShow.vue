<template>
    <NoteOwnerBreadcrumb v-bind="noteViewedByUser"/>
    <Card :note="noteViewedByUser.note">
        <template #button="{note}">
            <button class="source_btn btn btn-sm btn-secondary" data-toggle="collapse" :data-target="`#note-collapse-${note.id}`" aria-expanded="false">
                Toggle Details
            </button>
        </template>
    </Card>
    <div :id="`note-collapse-${noteViewedByUser.note.id}`" class="collapse">
        <NoteShow :note="note" :level="2" :forBazaar="false"/>
    </div>
</template>

<script setup>
import NoteOwnerBreadcrumb from "../notes/NoteOwnerBreadcrumb.vue"
import NoteShowWithTitle from "../notes/NoteShowWithTitle.vue"
import Card from "../notes/Card.vue"
import { defineProps } from "vue"

  const props = defineProps({ noteViewedByUser: Object })
</script>
