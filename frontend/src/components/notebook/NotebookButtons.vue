<template>
  <div class="btn-group btn-group-sm">
    <slot name="additional-buttons" />
    <NotebookEditButton v-bind="{ notebook }" />
    <NotebookCopyButton v-if="featureToggle" v-bind="{ notebook }" />
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
import NotebookCopyButton from "./NotebookCopyButton.vue";
import NotebookEditButton from "./NotebookEditButton.vue";
import SvgBazaarShare from "../svgs/SvgBazaarShare.vue";
import { restPost } from "../../restful/restful";

export default {
  props: { notebook: Object, featureToggle: Boolean  },
  components: { NotebookEditButton, SvgBazaarShare, NotebookCopyButton },
  methods: {
    async shareNotebook() {
      if (await this.$popups.confirm(`Are you sure to share?`)) {
        restPost(
          `/api/notebooks/${this.notebook.id}/share`,
          {},
          (r) => {}
        ).then((r) => this.$router.push({ name: "notebooks" }));
      }
    },
  },
};
</script>
