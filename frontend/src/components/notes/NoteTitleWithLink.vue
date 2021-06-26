<template>
  <template v-if="!!note">
    <RelativeRouterLink :to="{name: 'noteShow', params: {noteid: note.id}}">
      {{note.title}}
    </RelativeRouterLink>
  </template>
</template>

<script setup>
  import RelativeRouterLink from "../RelativeRouterLink.vue"
  const props = defineProps({note: Object})
</script>
