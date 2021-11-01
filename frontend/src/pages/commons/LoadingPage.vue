<template>
  <LoadingThinBar v-if="loading" />
  <div class="container" v-if="inContainer">
    <slot v-if="contentExists" />
    <div v-else><ContentLoader /></div>
  </div>
  <template v-else>
    <slot v-if="contentExists" />
    <div v-else><ContentLoader /></div>
  </template>
</template>

<script setup>
import ContentLoader from "../../components/commons/ContentLoader.vue";
import LoadingThinBar from "../../components/commons/LoadingThinBar.vue";

const props = defineProps({
   loading: Boolean,
   contentExists: Boolean,
   inContainer: { type: Boolean, default: false},
  });
</script>
