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

<script lang="ts">
import { defineComponent } from "vue";
import NotebookEditButton from "./NotebookEditButton.vue";
import SvgBazaarShare from "../svgs/SvgBazaarShare.vue";
import useLoadingApi from '../../managedApi/useLoadingApi';
import usePopups from "../commons/Popups/usePopup";

export default defineComponent({
  setup() {
    return {...useLoadingApi(), ...usePopups()}
  },
  props: { notebook: Object },
  components: { NotebookEditButton, SvgBazaarShare },
  methods: {
    async shareNotebook() {
      if (await this.popups.confirm(`Are you sure to share?`)) {
        this.api.shareToBazaar(this.notebook.id)
        .then((r) => this.$router.push({ name: "notebooks" }));
      }
    },
  },
});
</script>
