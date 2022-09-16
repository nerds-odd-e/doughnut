<template>
  <slot v-if="contentExists" />
  <div v-else><ContentLoader /></div>
</template>

<script lang="ts">
import ContentLoader from "../../components/commons/ContentLoader.vue";

export default {
  props: {
    contentExists: Boolean,
  },
  components: { ContentLoader },
};
</script>
