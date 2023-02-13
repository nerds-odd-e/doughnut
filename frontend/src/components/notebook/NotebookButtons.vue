<template>
  <div class="btn-group btn-group-sm">
    <slot name="additional-buttons" />
    <PopupButton title="Edit notebook settings">
      <template #button_face>
        <SvgEditNotebook />
      </template>
      <template #dialog_body="{ doneHandler }">
        <NotebookEditDialog v-bind="{ notebook }" @done="doneHandler($event)" />
      </template>
    </PopupButton>
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
import { defineComponent, PropType } from "vue";
import SvgBazaarShare from "../svgs/SvgBazaarShare.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import usePopups from "../commons/Popups/usePopups";
import PopupButton from "../commons/Popups/PopupButton.vue";
import NotebookEditDialog from "./NotebookEditDialog.vue";
import SvgEditNotebook from "../svgs/SvgEditNotebook.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi(), ...usePopups() };
  },
  props: {
    notebook: { type: Object as PropType<Generated.Notebook>, required: true },
  },
  components: {
    SvgBazaarShare,
    PopupButton,
    NotebookEditDialog,
    SvgEditNotebook,
  },
  methods: {
    async shareNotebook() {
      if (await this.popups.confirm(`Confirm to share?`)) {
        this.api
          .shareToBazaar(this.notebook.id)
          .then(() => this.$router.push({ name: "notebooks" }));
      }
    },
  },
});
</script>
