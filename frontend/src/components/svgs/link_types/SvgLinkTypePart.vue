<template>
  <svg x="0px" y="0px" viewBox="0 0 800 400">
    <ObjectLeft
      link-role="source"
      :x="ifInverse === 1 ? -330 : 175"
      :oheight="300"
      :y="50"
    />
    <ObjectRight
      link-role="source"
      :x="ifInverse === 1 ? -175 : 330"
      :oheight="300"
      :y="50"
    />
    <Lnk />
    <ObjectRight link-role="target" :x="ifInverse === 1 ? 275 : -200" />
  </svg>
</template>

<script setup>
defineProps({ ifInverse: { type: Number, default: 1 } })
</script>
