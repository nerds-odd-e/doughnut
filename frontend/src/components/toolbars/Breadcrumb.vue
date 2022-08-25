<template>
  <BreadcrumbWithCircle v-bind="{ ancestors, circle, fromBazaar }">
    <slot />
  </BreadcrumbWithCircle>
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
