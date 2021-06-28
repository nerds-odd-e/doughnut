<template>
    <router-link :to="relativeRoute({$route}, to)">
      <slot />
    </router-link>
</template>

<script setup>
  import { relativeRoute } from './relative_routes'
  const props = defineProps({to: Object})
</script>
