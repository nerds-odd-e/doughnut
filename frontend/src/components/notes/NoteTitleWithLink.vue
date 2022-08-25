<template>
  <router-link :to="computedRouteTo" class="text-decoration-none">
    {{ note.title }}
  </router-link>
</template>

<script>
import { routeNameForViewType } from "../../models/viewTypes";

export default {
  props: {
    note: { type: Object, required: true },
    viewType: String,
  },
  computed: {
    computedRouteTo() {
      return {
        name: routeNameForViewType(this.computedViewType),
        params: { noteId: this.note.id },
      };
    },
    computedViewType() {
      if (this.viewType) {
        return this.viewType;
      }
      return this.$route?.meta?.viewType;
    },
  },
};
</script>
