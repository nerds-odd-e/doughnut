<template>
  <LoadingThinBar v-if="loading"/>
  <slot v-if="contentExists"/>
  <div v-else><ContentLoader /></div>
</template>

<script setup>
import ContentLoader from "../../components/ContentLoader.vue"
import LoadingThinBar from "../../components/LoadingThinBar.vue"

const props = defineProps({loading: Boolean, contentExists: Boolean})
</script>
