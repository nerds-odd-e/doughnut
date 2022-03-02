<template>
  <div class="btn-group btn-group-sm">
    <slot name="additional-buttons" />
    <NotebookEditButton v-bind="{ notebook }" />
    <button
      class="btn btn-sm"
      title="Share notebook to bazaar"
      @click="shareNotebook()"
    >
      <SvgBazaarShare />
    </button>
  </div>
</template>

<script>
import NotebookEditButton from "./NotebookEditButton.vue";
import SvgBazaarShare from "../svgs/SvgBazaarShare.vue";
import { restPost } from "../../restful/restful";

export default {
  props: { notebook: Object },
  components: { NotebookEditButton, SvgBazaarShare },
  methods: {
    async shareNotebook() {
      if (await this.$popups.confirm(`Are you sure to share?`)) {
        restPost(
          `/api/notebooks/${this.notebook.id}/share`,
          {},
        ).then((r) => this.$router.push({ name: "notebooks" }));
      }
    },
  },
};
</script>
