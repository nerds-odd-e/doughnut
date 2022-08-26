<template>
  <h3>Link</h3>
  <div v-if="!!inverseIcon">
    Source:
    <strong>
      <NoteTitleWithLink
        class="link-title"
        v-bind="{ note: link.sourceNote }"
      />
    </strong>
  </div>
  <LinkTypeSelect
    field="linkType"
    scope-name="link"
    v-model="formData.linkType"
    :errors="formErrors.linkType"
    :inverse-icon="true"
  />
  <div v-if="!inverseIcon">
    Target:
    <strong>
      <NoteTitleWithLink
        class="link-title"
        v-bind="{ note: link.targetNote }"
      />
    </strong>
  </div>

  <button class="btn btn-primary" @click="updateLink()">Update</button>
  <button class="btn btn-danger" @click="deleteLink()">Delete</button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkTypeSelect from "./LinkTypeSelect.vue";
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import usePopups from "../commons/Popups/usePopup";
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  setup(props) {
    return {
      ...useStoredLoadingApi({
        undoHistory: props.historyWriter,
        hasFormError: true,
      }),
      ...usePopups(),
    };
  },
  props: {
    link: {
      type: Object as PropType<Generated.Link>,
      required: true,
    },
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
    },
    inverseIcon: Boolean,
    colors: Object,
  },
  emits: ["done", "linkDeleted"],
  components: {
    LinkTypeSelect,
    NoteTitleWithLink,
  },
  data() {
    return {
      formData: {
        linkType: this.link.linkType,
      } as Generated.LinkCreation,
      formErrors: { linkType: undefined },
    };
  },

  methods: {
    async updateLink() {
      await this.storedApi.updateLink(this.link.id, this.formData);
      this.$emit("done");
    },

    async deleteLink() {
      if (!(await this.popups.confirm("Confirm to delete this link?"))) {
        this.$emit("done", null);
        return;
      }
      await this.storedApi.deleteLink(this.link.id);
      this.$emit("done");
    },
  },
});
</script>

<style scoped>
.link-nob {
  padding: 3px;
}
</style>
