<template>
<div class="btn-group btn-group-sm">
    <NoteNavigationButton  :noteid="navigation.previousSiblingId" :title="'previous sibling'">
        <SvgFastBackward />
    </NoteNavigationButton>
    <NoteNavigationButton  :noteid="navigation.previousId" :title="'previous'">
        <SvgBackward />
    </NoteNavigationButton>
    <NoteNavigationButton  :noteid="navigation.nextId" :title="'next'">
        <SvgForward />
    </NoteNavigationButton>
    <NoteNavigationButton  :noteid="navigation.nextSiblingId" :title="'next sibling'">
        <SvgFastForward />
    </NoteNavigationButton>
</div>
</template>

<script setup>
  import SvgFastBackward from "../svgs/SvgFastBackward.vue"
  import SvgBackward from "../svgs/SvgBackward.vue"
  import SvgForward from "../svgs/SvgForward.vue"
  import SvgFastForward from "../svgs/SvgFastForward.vue"
  import NoteNavigationButton from "./NoteNavigationButton.vue"
  const props = defineProps({navigation: Object})
</script>

