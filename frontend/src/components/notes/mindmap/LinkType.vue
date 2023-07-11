<template>
  <g class="notes-links">
    <g
      class="link-start"
      :transform="`translate(${linkStart.x}, ${linkStart.y}) rotate(${
        (linkStart.angle * 180) / Math.PI
      })`"
    >
      <g transform="translate(0, -10)">
        <SvgLinkTypeIcon
          width="40"
          height="20"
          :link-type="linkType"
          :inverse-icon="!reverse"
        />
      </g>
    </g>
  </g>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import MindmapSector from "../../../models/MindmapSector";
import Mindmap from "../../../models/Mindmap";
import SvgLinkTypeIcon from "../../svgs/SvgLinkTypeIcon.vue";

export default defineComponent({
  props: {
    reverse: { type: Boolean, default: false },
    linkType: { type: String as PropType<Generated.LinkType>, required: true },
    totalLinkTypeCount: { type: Number, required: true },
    index: { type: Number, required: true },
    mindmapSector: { type: Object as PropType<MindmapSector>, required: true },
    mindmap: { type: Object as PropType<Mindmap>, required: true },
  },
  components: { SvgLinkTypeIcon },
  computed: {
    linkStart() {
      if (this.reverse)
        return this.mindmap.inSlot(
          this.mindmapSector,
          this.totalLinkTypeCount,
          this.index,
        );
      return this.mindmap.outSlot(
        this.mindmapSector,
        this.totalLinkTypeCount,
        this.index,
      );
    },
  },
  methods: {},
});
</script>

<style lang="sass" scoped></style>
