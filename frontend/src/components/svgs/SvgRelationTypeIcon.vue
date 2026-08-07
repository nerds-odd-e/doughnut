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
import SvgFolder from "./relation_types/SvgFolder.vue"
import SvgRelationTypeApplication from "./relation_types/SvgRelationTypeApplication.vue"
import SvgRelationTypeAttr from "./relation_types/SvgRelationTypeAttr.vue"
import SvgRelationTypeAuthor from "./relation_types/SvgRelationTypeAuthor.vue"
import SvgRelationTypePrecede from "./relation_types/SvgRelationTypeBefore.vue"
import SvgRelationTypeConfuse from "./relation_types/SvgRelationTypeConfuse.vue"
import SvgRelationTypeExample from "./relation_types/SvgRelationTypeExample.vue"
import SvgRelationTypeInstance from "./relation_types/SvgRelationTypeInstance.vue"
import SvgRelationTypeOpposite from "./relation_types/SvgRelationTypeOpposite.vue"
import SvgRelationTypePart from "./relation_types/SvgRelationTypePart.vue"
import SvgRelationTypeRelated from "./relation_types/SvgRelationTypeRelated.vue"
import SvgRelationTypeSimilar from "./relation_types/SvgRelationTypeSimilar.vue"
import SvgRelationTypeSpecialize from "./relation_types/SvgRelationTypeSpecialize.vue"
import SvgRelationTypeTagged from "./relation_types/SvgRelationTypeTag.vue"
import SvgRelationTypeUse from "./relation_types/SvgRelationTypeUse.vue"

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
      if (relationType === "related to") return SvgRelationTypeRelated
      if (relationType === "a specialization of")
        return SvgRelationTypeSpecialize
      if (relationType === "an application of")
        return SvgRelationTypeApplication
      if (relationType === "an instance of") return SvgRelationTypeInstance
      if (relationType === "a part of") return SvgRelationTypePart
      if (relationType === "tagged by") return SvgRelationTypeTagged
      if (relationType === "an attribute of") return SvgRelationTypeAttr
      if (relationType === "the opposite of") return SvgRelationTypeOpposite
      if (relationType === "author of") return SvgRelationTypeAuthor
      if (relationType === "using") return SvgRelationTypeUse
      if (relationType === "an example of") return SvgRelationTypeExample
      if (relationType === "before") return SvgRelationTypePrecede
      if (relationType === "similar to") return SvgRelationTypeSimilar
      if (relationType === "confused with") return SvgRelationTypeConfuse
      return SvgFolder
    },
  },
})
</script>
