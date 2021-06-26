<template>
  <RelativeRouterLink v-if="!!owns" :to="{name: 'linkShow', params: {linkid: link.id}}">{{note.title}}</RelativeRouterLink>
  <NoteTitleWithLink v-else v-bind="{note}"/>
</template>

<script setup>
  import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
  import RelativeRouterLink from "../RelativeRouterLink.vue"
  const props = defineProps({note: Object, link: Object, owns: Boolean})
</script>
