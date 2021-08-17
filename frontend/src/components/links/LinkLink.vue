<template>
  <span>
    <LinkNob v-bind="{owns, link}" v-if="!!reverse" :inverseIcon="true"/>
    <NoteTitleWithLink :class="`badge ml-1 mr-1 ${reverse ? 'badge-warning' : 'badge-light'}`" v-bind="{note}"/>
    <LinkNob v-bind="{owns, link}" v-if="!reverse" :inverseIcon="false"/>
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