<script setup>
import NoteStatisticsButton from './components/NoteStatisticsButton.vue'
import { inject } from 'vue'

const noteid = inject('noteid')
</script>

<template>
  <NoteStatisticsButton :noteid="noteid"/>
</template>
