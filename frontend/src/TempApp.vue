<script setup>
import NoteShow from './components/NoteShow.vue'
import NoteStatisticsButton from './components/NoteStatisticsButton.vue'
import { inject } from 'vue'

const noteid = inject('noteid')
</script>

<template>
  <NoteShow :noteid="noteid" :level="1" :forBazaar="false"/>
  <NoteStatisticsButton :noteid="noteid"/>
</template>
