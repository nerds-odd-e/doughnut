<template>
  <span class="link-nob">
    <router-link :disabled="!owns" :to="{name: 'linkShow', params: {linkid: link.id}}">
      <SvgLinkTypeIcon :linkTypeId="link.typeId" />
    </router-link>
  </span>
</template>

<script setup>
  import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"
  const props = defineProps({ link: Object, owns: Boolean})
</script>

<style lang="sass" scoped>

</style>