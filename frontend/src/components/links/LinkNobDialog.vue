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
    :errors="linkFormErrors.linkType"
    :inverse-icon="true"
    @update:model-value="updateLink"
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

  <button class="btn btn-danger" @click="deleteLink()">Delete</button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkTypeSelect from "./LinkTypeSelect.vue";
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue";
import usePopups from "../commons/Popups/usePopups";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return { ...usePopups() };
  },
  props: {
    link: {
      type: Object as PropType<Generated.Link>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
    inverseIcon: Boolean,
    colors: Object,
  },
  emits: ["closeDialog"],
  components: {
    LinkTypeSelect,
    NoteTitleWithLink,
  },
  data() {
    return {
      formData: {
        linkType: this.link.linkType,
        fromTargetPerspective: this.inverseIcon,
      } as Generated.LinkCreation,
      linkFormErrors: { linkType: undefined as string | undefined },
    };
  },

  methods: {
    updateLink() {
      this.storageAccessor
        .api(this.$router)
        .updateLink(this.link.id, this.formData)
        .then(() => this.$emit("closeDialog"))
        .catch((error) => {
          this.linkFormErrors = error;
        });
    },

    async deleteLink() {
      if (!(await this.popups.confirm("Confirm to delete this link?"))) {
        this.$emit("closeDialog");
        return;
      }
      await this.storageAccessor
        .api(this.$router)
        .deleteLink(this.link.id, this.inverseIcon);
      this.$emit("closeDialog");
    },
  },
});
</script>

<style scoped>
.link-nob {
  padding: 3px;
}
</style>
