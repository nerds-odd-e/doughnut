<script setup>
import NoteStatisticsButton from './components/NoteStatisticsButton.vue'
import { inject, defineProps, onBeforeMount, getCurrentInstance } from 'vue'

const internalInstance = getCurrentInstance()
const noteid = inject('noteid')
</script>

<template>
  <NoteStatisticsButton :noteid="noteid"/>
</template>
