<template>
  <span>
    <LinkNob class="link-nob" v-bind="{owns, link}" :inverseIcon="!reverse"/>
    <NoteTitleWithLink :class="`badge ml-1 mr-1 ${reverse ? 'badge-warning' : 'badge-light'}`" v-bind="{note}"/>
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
.link-nob
  margin-right: -5px
  padding: 3px
  border-radius: 10px
  border: solid 1px #000

</style>