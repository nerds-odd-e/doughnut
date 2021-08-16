<template>
  <span class="link-nob">
    <router-link :disabled="!owns" :to="{name: 'linkShow', params: {linkid: link.id}}">
      <SvgLinkTypeIcon :linkTypeId="link.typeId" :inverseIcon="inverseIcon" />
    </router-link>
  </span>
</template>

<script setup>
  import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"
  const props = defineProps({ link: Object, owns: Boolean, inverseIcon: Boolean})
</script>

<style lang="sass" scoped>

</style>