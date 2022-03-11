<template>
  <LoadingThinBar v-if="loading" />
  <slot v-if="contentExists" />
  <div v-else><ContentLoader /></div>
</template>

<script lang="ts">
import ContentLoader from "../../components/commons/ContentLoader.vue";
import LoadingThinBar from "../../components/commons/LoadingThinBar.vue";

export default {
  props: {
   loading: Boolean,
   contentExists: Boolean,
  },
  components: { ContentLoader, LoadingThinBar }
}
</script>
