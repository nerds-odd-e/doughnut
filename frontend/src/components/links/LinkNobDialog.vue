<template>
  <h3>Link</h3>
  <div v-if="!!inverseIcon">
    Source:
    <strong>
      <NoteTopicWithLink
        v-if="link.sourceNote"
        class="link-title"
        v-bind="{ noteTopic: link.sourceNote.noteTopic }"
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
      <NoteTopicWithLink
        v-if="link.targetNote"
        class="link-title"
        v-bind="{ noteTopic: link.targetNote.noteTopic }"
      />
    </strong>
  </div>

  <button class="btn btn-danger" @click="deleteLink()">Delete</button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { LinkCreation, Thing } from "@/generated/backend";
import LinkTypeSelect from "./LinkTypeSelect.vue";
import NoteTopicWithLink from "../notes/NoteTopicWithLink.vue";
import usePopups from "../commons/Popups/usePopups";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  setup() {
    return { ...usePopups() };
  },
  props: {
    link: {
      type: Object as PropType<Thing>,
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
    NoteTopicWithLink,
  },
  data() {
    return {
      formData: {
        linkType: this.link.note?.linkType,
        fromTargetPerspective: this.inverseIcon,
      } as LinkCreation,
      linkFormErrors: { linkType: undefined as string | undefined },
    };
  },

  methods: {
    updateLink() {
      this.storageAccessor
        .storedApi()
        .updateLink(this.link.note!.id, this.formData)
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
        .storedApi()
        .deleteLink(this.link.note!.id, this.inverseIcon);
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
