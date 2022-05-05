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
          :link-type-name="linkTypeName"
          :inverse-icon="!reverse"
        />
      </g>
    </g>
  </g>
</template>

<script>
import SvgLinkTypeIcon from "../../svgs/SvgLinkTypeIcon.vue";

export default {
  props: {
    reverse: { type: Boolean, default: false },
    linkTypeName: String,
    links: Array,
    totalLinkTypeCount: Number,
    index: Number,
    mindmapSector: Object,
    mindmap: Object,
  },
  components: { SvgLinkTypeIcon },
  computed: {
    linkStart() {
      if (this.reverse)
        return this.mindmap.inSlot(
          this.mindmapSector,
          this.totalLinkTypeCount,
          this.index
        );
      return this.mindmap.outSlot(
        this.mindmapSector,
        this.totalLinkTypeCount,
        this.index
      );
    },
  },
  methods: {},
};
</script>

<style lang="sass" scoped></style>
