<script setup>
import NoteShowPage from './components/notes/NoteShowPage.vue'
import NoteStatisticsButton from './components/notes/NoteStatisticsButton.vue'
import { inject } from 'vue'

const props = defineProps({noteid: Number})
</script>

<template>
  <NoteShowPage :noteid="noteid" :level="1" :forBazaar="false"/>
  <NoteStatisticsButton :noteid="noteid"/>
</template>
