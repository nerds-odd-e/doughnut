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
import { defineComponent } from "vue"
import { LinkCreation } from "@generated/backend"
import SvgFolder from "./link_types/SvgFolder.vue"
import SvgLinkTypeApplication from "./link_types/SvgLinkTypeApplication.vue"
import SvgLinkTypeAttr from "./link_types/SvgLinkTypeAttr.vue"
import SvgLinkTypeAuthor from "./link_types/SvgLinkTypeAuthor.vue"
import SvgLinkTypePrecede from "./link_types/SvgLinkTypeBefore.vue"
import SvgLinkTypeConfuse from "./link_types/SvgLinkTypeConfuse.vue"
import SvgLinkTypeExample from "./link_types/SvgLinkTypeExample.vue"
import SvgLinkTypeInstance from "./link_types/SvgLinkTypeInstance.vue"
import SvgLinkTypeOpposite from "./link_types/SvgLinkTypeOpposite.vue"
import SvgLinkTypePart from "./link_types/SvgLinkTypePart.vue"
import SvgLinkTypeRelated from "./link_types/SvgLinkTypeRelated.vue"
import SvgLinkTypeSimilar from "./link_types/SvgLinkTypeSimilar.vue"
import SvgLinkTypeSpecialize from "./link_types/SvgLinkTypeSpecialize.vue"
import SvgLinkTypeTagged from "./link_types/SvgLinkTypeTag.vue"
import SvgLinkTypeUse from "./link_types/SvgLinkTypeUse.vue"

export default defineComponent({
  props: {
    linkType: { type: String, required: true },
    width: String,
    height: String,
    inverseIcon: Boolean,
  },

  computed: {
    iconComponent() {
      const { linkType } = this
      if (linkType === LinkCreation.linkType.RELATED_TO)
        return SvgLinkTypeRelated
      if (linkType === LinkCreation.linkType.A_SPECIALIZATION_OF)
        return SvgLinkTypeSpecialize
      if (linkType === LinkCreation.linkType.AN_APPLICATION_OF)
        return SvgLinkTypeApplication
      if (linkType === LinkCreation.linkType.AN_INSTANCE_OF)
        return SvgLinkTypeInstance
      if (linkType === LinkCreation.linkType.A_PART_OF) return SvgLinkTypePart
      if (linkType === LinkCreation.linkType.TAGGED_BY) return SvgLinkTypeTagged
      if (linkType === LinkCreation.linkType.AN_ATTRIBUTE_OF)
        return SvgLinkTypeAttr
      if (linkType === LinkCreation.linkType.THE_OPPOSITE_OF)
        return SvgLinkTypeOpposite
      if (linkType === LinkCreation.linkType.AUTHOR_OF) return SvgLinkTypeAuthor
      if (linkType === LinkCreation.linkType.USING) return SvgLinkTypeUse
      if (linkType === LinkCreation.linkType.AN_EXAMPLE_OF)
        return SvgLinkTypeExample
      if (linkType === LinkCreation.linkType.BEFORE) return SvgLinkTypePrecede
      if (linkType === LinkCreation.linkType.SIMILAR_TO)
        return SvgLinkTypeSimilar
      if (linkType === LinkCreation.linkType.CONFUSED_WITH)
        return SvgLinkTypeConfuse
      return SvgFolder
    },
  },
})
</script>
