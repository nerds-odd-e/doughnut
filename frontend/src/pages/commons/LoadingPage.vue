<template>
  <slot v-if="contentExists" />
  <div v-else><ContentLoader /></div>
</template>

<script lang="ts">
import { PropType } from "vue";
import ContentLoader from "../../components/commons/ContentLoader.vue";

export default {
  props: {
    contentExists: null as unknown as PropType<
      Record<number, string>[] | boolean | null | undefined
    >,
  },
  components: { ContentLoader },
};
</script>
