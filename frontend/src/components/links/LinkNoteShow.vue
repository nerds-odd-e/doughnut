<template>
    <NoteOwnerBreadcrumb v-bind="noteViewedByUser"/>
    <div th:id="'note-collapse-' + ${note.id}" class="collapse" th:insert="_fragments/note_fragments :: noteShow(${note}, 2, false)"/>
</template>

<script setup>
import NoteOwnerBreadcrumb from "../notes/NoteOwnerBreadcrumb.vue"
import NoteShowWithTitle from "../notes/NoteShowWithTitle.vue"
import Card from "../notes/Card.vue"
import { defineProps } from "vue"

  const props = defineProps({ noteViewedByUser: Object })
</script>
