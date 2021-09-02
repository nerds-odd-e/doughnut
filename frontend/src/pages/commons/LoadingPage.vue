<template>
  <LoadingThinBar v-if="loading" />
  <slot v-if="contentExists" />
  <div v-else><ContentLoader /></div>
</template>

<script setup>
import ContentLoader from "../../components/commons/ContentLoader.vue";
import LoadingThinBar from "../../components/commons/LoadingThinBar.vue";

const props = defineProps({ loading: Boolean, contentExists: Boolean });
</script>
