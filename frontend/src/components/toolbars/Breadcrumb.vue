<template>
  <BreadcrumbWithCircle v-bind="{ ancestors, circle: cCircle, fromBazaar }">
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
    circle: Object as PropType<Generated.Circle>,
  },
  components: { BreadcrumbWithCircle },
  computed: {
    fromBazaar() {
      return this.notebook?.fromBazaar;
    },
    cCircle() {
      if (this.circle) return this.circle;
      return this.notebook ? this.notebook.ownership.circle : undefined;
    },
  },
});
</script>
