<template>

    <span :class="`badge ml-1 mr-1 ${reverse ? 'badge-warning' : 'badge-light'}`">
      <LinkNob v-bind="{owns, link}" v-if="!reverse"/>
      <NoteTitleWithLink v-bind="{note}"/>
      <LinkNob v-bind="{owns, link}" v-if="reverse"/>
    </span>
</template>

<script setup>
  import { computed } from "@vue/runtime-core"
  import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
  import LinkNob from "./LinkNob.vue"

  const props = defineProps({ link: Object, reverse: Boolean, owns: Boolean})
  const note = computed(()=>!!props.reverse ? props.link.sourceNote : props.link.targetNote)
</script>

<style lang="sass" scoped>

</style>