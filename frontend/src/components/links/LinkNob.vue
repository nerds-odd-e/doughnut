<template>
  <span class="link-nob">
    <router-link :disabled="!owns" :to="{name: 'linkShow', params: {linkid: link.id}}" :title="link.linkTypeLabel">
      <SvgLinkTypeIcon :linkTypeId="link.typeId" :inverseIcon="inverseIcon" />
    </router-link>
  </span>
</template>

<script setup>
  import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue"
  const props = defineProps({ link: Object, owns: Boolean, inverseIcon: Boolean, colors: Object})
</script>

<style scoped>

.link-nob {
  padding: 3px;
}
</style>