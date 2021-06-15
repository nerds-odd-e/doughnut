<template>
<div>
    <ol class="breadcrumb">
        <slot name="topLink" />
        <li class="breadcrumb-item"
            v-for="note in ancestors"
            :key= "note.id">
          <component v-if="!!linkFragment" :is="linkFragment" :note="note" />
          <NoteTitleWithLink v-else :note="note" />
        </li>
        <slot name="additional" />
    </ol>
</div>
</template>

<script setup>
  import NoteTitleWithLink from "./NoteTitleWithLink.vue"
  const props = defineProps({ancestors: Array, linkFragment: Object})
</script>
