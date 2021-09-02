<template>
  <ModalWithButton v-model="show">
    <template #button>
      <span class="link-nob">
        <a
          role="button"
          :disabled="!owns"
          @click="show = true"
          :title="link.linkTypeLabel"
        >
          <SvgLinkTypeIcon
            :linkTypeId="link.typeId"
            :inverseIcon="inverseIcon"
          />
        </a>
      </span>
    </template>
    <template #header>
      <h3>Link</h3>
    </template>
    <template #body>
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
        scopeName="link"
        v-model="formData.typeId"
        :errors="formErrors.typeId"
        :inverseIcon="true"
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

      <button class="btn btn-primary" v-on:click="updateLink()">Update</button>
      <button class="btn btn-danger" v-on:click="deleteLink()">Delete</button>
    </template>
  </ModalWithButton>
</template>

<script>
import SvgLinkTypeIcon from "../svgs/SvgLinkTypeIcon.vue";
import ModalWithButton from "../commons/ModalWithButton.vue";
import LinkTypeSelect from "./LinkTypeSelect.vue";
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue";
import { restGet, restPost } from "../../restful/restful";

export default {
  props: { link: Object, owns: Boolean, inverseIcon: Boolean, colors: Object },
  emits: ["updated"],
  components: {
    SvgLinkTypeIcon,
    ModalWithButton,
    LinkTypeSelect,
    NoteTitleWithLink,
  },
  data() {
    return {
      show: false,
      loading: false,
      formData: { typeId: this.link.typeId },
      formErrors: {},
    };
  },

  methods: {
    updateLink() {
      restPost(
        `/api/links/${this.link.id}`,
        this.formData,
        (r) => (this.loading = r)
      )
        .then((res) => {
          this.$emit("updated");
          this.show = false;
        })
        .catch((res) => (this.formErrors = res));
    },

    async deleteLink() {
      if (!(await this.$popups.confirm("Are you sure to delete this link?")))
        return;
      restPost(
        `/api/links/${this.link.id}/delete`,
        null,
        (r) => (this.loading = r)
      )
        .then((res) => {
          this.$emit("updated");
          this.show = false;
        })
        .catch((res) => (this.formErrors = res));
    },
  },
};
</script>

<style scoped>
.link-nob {
  padding: 3px;
}
</style>
