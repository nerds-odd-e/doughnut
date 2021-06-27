<template>
    <router-link :to="{...to, name: `${prefix($router)}${to.name}`}">
      <slot />
    </router-link>
</template>

<script setup>
  import {relativeRoute} from "../relative_routes"
  const props = defineProps({to: Object})
  const prefix = (router) => {
    const currentRouteName = router.currentRoute._rawValue.name
    if(!!currentRouteName) {
      if(currentRouteName.split("-").shift() === 'repeat') return 'repeat-'
    }
    return ''
  }
</script>
