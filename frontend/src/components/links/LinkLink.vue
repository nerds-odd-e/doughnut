<template>

  <span class="link-link">
    <LinkNob v-bind="{owns, link, colors}" v-if="!!reverse" :inverseIcon="true"/>
    <NoteTitleWithLink class="link-title" v-bind="{note}"/>
    <LinkNob v-bind="{owns, link, colors}" v-if="!reverse" :inverseIcon="false"/>
  </span>

</template>

<script setup>
  import { computed } from "@vue/runtime-core"
  import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue"
  import LinkNob from "./LinkNob.vue"

  const props = defineProps({ link: Object, reverse: Boolean, owns: Boolean, colors: {Object, required: true}})
  const note = computed(()=>!!props.reverse ? props.link.sourceNote : props.link.targetNote)
  const bgcolor = computed(()=>!!props.reverse ? props.colors['target'] : props.colors['source'])
</script>

<style lang="sass" scoped>
  .link-link
    border-bottom: solid 1px black
    padding-bottom: 3px
    border-radius: 10px
    background-color: v-bind(bgcolor)
    margin-right: 10px

  .link-title
    padding-bottom: 3px

</style>