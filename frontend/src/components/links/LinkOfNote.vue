<template>
  <span class="link-link">
    <LinkNob
      v-bind="{ link, colors, storageAccessor }"
      v-if="!!reverse"
      :inverse-icon="true"
    />
    <NoteTopicConstructorWithLink class="link-title" v-bind="{ note }" />
    <LinkNob
      v-bind="{ link, colors, storageAccessor }"
      v-if="!reverse"
      :inverse-icon="false"
    />
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteTopicConstructorWithLink from "../notes/NoteTopicConstructorWithLink.vue";
import LinkNob from "./LinkNob.vue";
import { colors } from "../../colors";
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    link: { type: Object as PropType<Generated.Link>, required: true },
    reverse: Boolean,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { NoteTopicConstructorWithLink, LinkNob },
  computed: {
    note() {
      return this.reverse ? this.link.sourceNote : this.link.targetNote;
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
