<template>
  <span class="link-nob">
    <router-link :disabled="!owns" :to="{name: 'linkShow', params: {linkid: link.id}}" :title="link.linkTypeLabel">
      <SvgLinkTypeIcon :linkTypeId="link.typeId" :inverseIcon="inverseIcon" />
    </router-link>
  </span>
</template>

<script setup>
  import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"
  const props = defineProps({ link: Object, owns: Boolean, inverseIcon: Boolean})
</script>

<style lang="sass" scoped>

.link-nob
  margin-right: -5px
  padding: 3px
  border-radius: 10px
  border: solid 1px #000

</style>