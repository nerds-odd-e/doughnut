<template>
  <h3>Link</h3>
  <div v-if="!!inverseIcon">
    Source:
    <strong>
      <NoteTopicWithLink
        v-if="noteTopic.parentNoteTopic"
        class="link-title"
        v-bind="{ noteTopic: noteTopic.parentNoteTopic }"
      />
    </strong>
  </div>
  <LinkTypeSelect
    field="linkType"
    scope-name="link"
    v-model="formData.linkType"
    :error-message="linkFormErrors.linkType"
    :inverse-icon="true"
    @update:model-value="updateLink"
  />
  <div v-if="!inverseIcon">
    Target:
    <strong>
      <NoteTopicWithLink
        v-if="noteTopic.targetNoteTopic"
        class="link-title"
        v-bind="{ noteTopic: noteTopic.targetNoteTopic }"
      />
    </strong>
  </div>
</template>

<script lang="ts">
import { LinkCreation, NoteTopic } from "@/generated/backend"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import type { StorageAccessor } from "../../store/createNoteStorage"
import usePopups from "../commons/Popups/usePopups"
import NoteTopicWithLink from "../notes/NoteTopicWithLink.vue"
import LinkTypeSelect from "./LinkTypeSelect.vue"

export default defineComponent({
  setup() {
    return { ...usePopups() }
  },
  props: {
    noteTopic: {
      type: Object as PropType<NoteTopic>,
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
        linkType: this.noteTopic.linkType,
        fromTargetPerspective: this.inverseIcon,
      } as LinkCreation,
      linkFormErrors: { linkType: undefined as string | undefined },
    }
  },

  methods: {
    updateLink() {
      this.storageAccessor
        .storedApi()
        .updateLink(this.noteTopic.id, this.formData)
        .then(() => this.$emit("closeDialog"))
        .catch((error) => {
          this.linkFormErrors = error
        })
    },
  },
})
</script>

<style scoped>
.link-nob {
  padding: 3px;
}
</style>
