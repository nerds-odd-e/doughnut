<template>
    <ul>
        <template v-for="(linksOfType, linkType) in links" :key="linkType">
            <li v-if="!!linksOfType.direct.length>0">
                <LinkLink  v-for="link in linksOfType.direct" :key="link.id" :link="link" :reverse="false" :owns="owns"/>
            </li>
        </template>
    </ul>
</template>

<script setup>
  import LinkLink from "./LinkLink.vue"
  const props = defineProps({ links: Object, owns: Boolean })
</script>
