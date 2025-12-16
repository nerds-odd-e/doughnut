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
// Using string literals for relationType values
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
    relationType: { type: String, required: true },
    width: String,
    height: String,
    inverseIcon: Boolean,
  },

  computed: {
    iconComponent() {
      const { relationType } = this
      if (relationType === "related to") return SvgLinkTypeRelated
      if (relationType === "a specialization of") return SvgLinkTypeSpecialize
      if (relationType === "an application of") return SvgLinkTypeApplication
      if (relationType === "an instance of") return SvgLinkTypeInstance
      if (relationType === "a part of") return SvgLinkTypePart
      if (relationType === "tagged by") return SvgLinkTypeTagged
      if (relationType === "an attribute of") return SvgLinkTypeAttr
      if (relationType === "the opposite of") return SvgLinkTypeOpposite
      if (relationType === "author of") return SvgLinkTypeAuthor
      if (relationType === "using") return SvgLinkTypeUse
      if (relationType === "an example of") return SvgLinkTypeExample
      if (relationType === "before") return SvgLinkTypePrecede
      if (relationType === "similar to") return SvgLinkTypeSimilar
      if (relationType === "confused with") return SvgLinkTypeConfuse
      return SvgFolder
    },
  },
})
</script>
