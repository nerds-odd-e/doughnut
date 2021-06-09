<script setup>
import NoteStatisticsButton from './components/NoteStatisticsButton.vue'
import { inject, defineProps, onBeforeMount, getCurrentInstance } from 'vue'

const internalInstance = getCurrentInstance()
const noteid = inject('noteid')
</script>

<template>
this is {{noteid}}

  <NoteStatisticsButton :noteid="noteid"/>
</template>
