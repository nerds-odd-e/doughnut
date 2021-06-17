<template>
<div class="btn-group btn-group-sm">
    <NoteLinkButton  :noteid="navigation.previousSiblingId" :title="'previous sibling'">
        <SvgFastBackward />
    </NoteLinkButton>
    <NoteLinkButton  :noteid="navigation.previousId" :title="'previous'">
        <SvgBackward />
    </NoteLinkButton>
    <NoteLinkButton  :noteid="navigation.nextId" :title="'next'">
        <SvgForward />
    </NoteLinkButton>
    <NoteLinkButton  :noteid="navigation.nextSiblingId" :title="'next sibling'">
        <SvgFastForward />
    </NoteLinkButton>
</div>
</template>

<script setup>
  import SvgFastBackward from "../svgs/SvgFastBackward.vue"
  import SvgBackward from "../svgs/SvgBackward.vue"
  import SvgForward from "../svgs/SvgForward.vue"
  import SvgFastForward from "../svgs/SvgFastForward.vue"
  import NoteLinkButton from "./NoteLinkButton.vue"
  const props = defineProps({navigation: Object})
</script>

