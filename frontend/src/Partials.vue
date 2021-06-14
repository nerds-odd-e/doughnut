<script setup>
import NoteStatisticsButton from './components/NoteStatisticsButton.vue'
import { inject, defineProps, onBeforeMount, getCurrentInstance } from 'vue'

const internalInstance = getCurrentInstance()
const noteid = inject('noteid')
const linkid = inject('linkid')
</script>

<template>
  <NoteStatisticsButton :noteid="noteid" :linkid="linkid"/>
</template>
