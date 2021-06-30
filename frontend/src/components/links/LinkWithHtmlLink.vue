<template>
  <router-link v-if="!!owns" :to="{name: 'linkShow', params: {linkid: link.id}}">{{note.title}}</router-link>
  <NoteTitleWithLink v-else v-bind="{note}"/>
</template>

<script setup>
  import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
  const props = defineProps({note: Object, link: Object, owns: Boolean})
</script>
