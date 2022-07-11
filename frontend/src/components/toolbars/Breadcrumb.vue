<template>
  <BreadcrumbWithCircle
    v-bind="{ ancestors, circle, fromBazaar }"
    @open-circle-selector="$emit('open-circle-selector')"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import BreadcrumbWithCircle from "./BreadcrumbWithCircle.vue";

export default defineComponent({
  name: "Breadcrumb",
  props: {
    ancestors: Array,
    notebook: Object as PropType<Generated.NotebookViewedByUser>,
  },
  emits: ["open-circle-selector"],
  components: { BreadcrumbWithCircle },
  computed: {
    fromBazaar() {
      return this.notebook?.fromBazaar;
    },
    circle() {
      return this.notebook ? this.notebook.ownership.circle : undefined;
    },
  },
});
</script>
