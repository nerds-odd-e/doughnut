<script setup>
import NoteStatisticsButton from './components/notes/NoteStatisticsButton.vue'
import { inject } from 'vue'

const noteid = inject('noteid')
const linkid = inject('linkid')
</script>

<template>
  <NoteStatisticsButton :noteid="noteid" :linkid="linkid"/>
</template>
