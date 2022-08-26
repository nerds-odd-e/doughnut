<template>
  <span class="link-link">
    <LinkNob
      v-bind="{ link, colors, historyWriter }"
      v-if="!!reverse"
      :inverse-icon="true"
      @link-deleted="$emit('noteRealmUpdated', $event)"
      @link-updated="$emit('noteRealmUpdated', $event)"
    />
    <NoteTitleWithLink class="link-title" v-bind="{ note }" />
    <LinkNob
      v-bind="{ link, colors, historyWriter }"
      v-if="!reverse"
      :inverse-icon="false"
      @link-deleted="$emit('noteRealmUpdated', $event)"
      @link-updated="$emit('noteRealmUpdated', $event)"
    />
  </span>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteTitleWithLink from "../notes/NoteTitleWithLink.vue";
import LinkNob from "./LinkNob.vue";
import { colors } from "../../colors";
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  props: {
    link: { type: Object as PropType<Generated.Link>, required: true },
    reverse: Boolean,
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
    },
  },
  emits: ["noteRealmUpdated"],
  components: { NoteTitleWithLink, LinkNob },
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
