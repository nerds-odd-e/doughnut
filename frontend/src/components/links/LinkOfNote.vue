<template>
  <span class="link-link">
    <LinkNob
      v-bind="{ link, colors, storageAccessor }"
      v-if="!!reverse"
      :inverse-icon="true"
    />
    <NoteTopicWithLink
      v-if="noteTopic"
      class="link-title"
      v-bind="{ noteTopic }"
    />
    <LinkNob
      v-bind="{ link, colors, storageAccessor }"
      v-if="!reverse"
      :inverse-icon="false"
    />
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Thing } from "@/generated/backend";
import NoteTopicWithLink from "../notes/NoteTopicWithLink.vue";
import LinkNob from "./LinkNob.vue";
import { colors } from "../../colors";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    link: { type: Object as PropType<Thing>, required: true },
    reverse: Boolean,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { NoteTopicWithLink, LinkNob },
  computed: {
    noteTopic() {
      return this.reverse
        ? this.link.note?.noteTopic.parentNoteTopic
        : this.link.note?.noteTopic.targetNoteTopic;
    },
    fontColor() {
      return this.reverse ? colors.target : colors.source;
    },
    colors() {
      return colors;
    },
  },
});
</script>

<style scoped>
.link-link {
  padding-bottom: 3px;
  margin-right: 10px;
}

.link-title {
  padding-bottom: 3px;
  color: v-bind(fontColor);
}
</style>
