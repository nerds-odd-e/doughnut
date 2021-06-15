<template>
  <a :href="`/links/${link.id}`">{{note.title}}</a>
</template>

<script setup>
  const props = defineProps({note: Object, link: Object})
</script>
