<template>
    <svg th:fragment="add" width="40px" height="20px">
        <component :is="iconComponent"/>
    </svg>
</template>

<script setup>
    import SvgLinkTypeSpecialize from './link_types/SvgLinkTypeSpecialize.vue';
    import SvgLinkTypeRelated from './link_types/SvgLinkTypeRelated.vue';
    import SvgLinkTypeApplication from './link_types/SvgLinkTypeApplication.vue';
    import SvgLinkTypeInstance from './link_types/SvgLinkTypeInstance.vue';
    import SvgLinkTypePart from './link_types/SvgLinkTypePart.vue';
    import SvgLinkTypeTagged from './link_types/SvgLinkTypeTag.vue';
    import SvgLinkTypeSimilar from './link_types/SvgLinkTypeSimilar.vue';
    import SvgLinkTypeExample from './link_types/SvgLinkTypeExample.vue';
    import SvgLinkTypePrecede from './link_types/SvgLinkTypeInstance.vue';
    import SvgLinkTypeUse from './link_types/SvgLinkTypeInstance.vue';
    import SvgLinkTypeAuthor from './link_types/SvgLinkTypeInstance.vue';
    import SvgLinkTypeOpposite from './link_types/SvgLinkTypeInstance.vue';
    import SvgLinkTypeAttr from './link_types/SvgLinkTypeInstance.vue';
    import SvgLinkTypeConfuse from './link_types/SvgLinkTypeInstance.vue';
import { computed } from '@vue/runtime-core';

  const props = defineProps({ linkTypeId: Number })
  const iconComponent = computed(()=> {
      const linkTypeId = props.linkTypeId
      if (linkTypeId === 1) return SvgLinkTypeRelated
      if (linkTypeId === 2) return SvgLinkTypeSpecialize
      if (linkTypeId === 3) return SvgLinkTypeApplication
      if (linkTypeId === 4) return SvgLinkTypeInstance
      if (linkTypeId === 6) return SvgLinkTypePart
      if (linkTypeId === 8) return SvgLinkTypeTagged
      if (linkTypeId === 10) return SvgLinkTypeAttr
      if (linkTypeId === 12) return SvgLinkTypeOpposite
      if (linkTypeId === 14) return SvgLinkTypeAuthor
      if (linkTypeId === 15) return SvgLinkTypeUse
      if (linkTypeId === 17) return SvgLinkTypeExample
      if (linkTypeId === 19) return SvgLinkTypePrecede
      if (linkTypeId === 22) return SvgLinkTypeSimilar
      if (linkTypeId === 23) return SvgLinkTypeConfuse
  })
</script>