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
    v-model="formData.typeId"
    :errors="formErrors.typeId"
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

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi({ hasFormError: true }), ...usePopups() };
  },
  props: {
    link: {
      type: Object as PropType<Generated.Link>,
      required: true,
    },
    inverseIcon: Boolean,
    colors: Object,
  },
  emits: ["done"],
  components: {
    LinkTypeSelect,
    NoteTitleWithLink,
  },
  data() {
    return {
      formData: {
        typeId: this.link.typeIdDeprecating,
      } as Generated.LinkRequest,
      formErrors: { typeId: undefined },
    };
  },

  methods: {
    async updateLink() {
      const res = await this.storedApi.updateLink(this.link.id, this.formData);
      this.$emit("done", res.notes[0]);
    },

    async deleteLink() {
      if (!(await this.popups.confirm("Confirm to delete this link?"))) {
        this.$emit("done", null);
        return;
      }
      const res = await this.storedApi.deleteLink(this.link.id);
      this.$emit("done", res.notes[0]);
    },
  },
});
</script>

<style scoped>
.link-nob {
  padding: 3px;
}
</style>
