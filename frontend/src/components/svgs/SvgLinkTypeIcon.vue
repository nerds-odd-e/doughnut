<template>
  <svg
    :width="width ? width : '40px'"
    :height="height ? height : `20px`"
    stroke="#000"
    stroke-width="5%"
  >
    <component :is="iconComponent" :if-inverse="!!inverseIcon ? -1 : 1" />
  </svg>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import SvgLinkTypeSpecialize from "./link_types/SvgLinkTypeSpecialize.vue";
import SvgLinkTypeRelated from "./link_types/SvgLinkTypeRelated.vue";
import SvgLinkTypeApplication from "./link_types/SvgLinkTypeApplication.vue";
import SvgLinkTypeInstance from "./link_types/SvgLinkTypeInstance.vue";
import SvgLinkTypePart from "./link_types/SvgLinkTypePart.vue";
import SvgLinkTypeTagged from "./link_types/SvgLinkTypeTag.vue";
import SvgLinkTypeSimilar from "./link_types/SvgLinkTypeSimilar.vue";
import SvgLinkTypeExample from "./link_types/SvgLinkTypeExample.vue";
import SvgLinkTypePrecede from "./link_types/SvgLinkTypeBefore.vue";
import SvgLinkTypeUse from "./link_types/SvgLinkTypeUse.vue";
import SvgLinkTypeAuthor from "./link_types/SvgLinkTypeAuthor.vue";
import SvgLinkTypeOpposite from "./link_types/SvgLinkTypeOpposite.vue";
import SvgLinkTypeAttr from "./link_types/SvgLinkTypeAttr.vue";
import SvgLinkTypeConfuse from "./link_types/SvgLinkTypeConfuse.vue";
import SvgFolder from "./link_types/SvgFolder.vue";

export default defineComponent({
  props: {
    linkTypeName: { type: String as PropType<Generated.LinkType>, required: true },
    width: String,
    height: String,
    inverseIcon: Boolean,
  },

  computed: {
    iconComponent() {
      const linkType = this.linkTypeName;
      if (linkType === "related to") return SvgLinkTypeRelated;
      if (linkType === "a specialization of") return SvgLinkTypeSpecialize;
      if (linkType === "an application of") return SvgLinkTypeApplication;
      if (linkType === "an instance of") return SvgLinkTypeInstance;
      if (linkType === "a part of") return SvgLinkTypePart;
      if (linkType === "tagged by") return SvgLinkTypeTagged;
      if (linkType === "an attribute of") return SvgLinkTypeAttr;
      if (linkType === "the opposite of") return SvgLinkTypeOpposite;
      if (linkType === "author of") return SvgLinkTypeAuthor;
      if (linkType === "using") return SvgLinkTypeUse;
      if (linkType === "an example of") return SvgLinkTypeExample;
      if (linkType === "before") return SvgLinkTypePrecede;
      if (linkType === "similar to") return SvgLinkTypeSimilar;
      if (linkType === "confused with") return SvgLinkTypeConfuse;
      return SvgFolder;
    },
  },
});
</script>
