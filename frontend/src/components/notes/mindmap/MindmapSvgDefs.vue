<template>
  <defs>
    <radialGradient id="myGradient">
      <stop offset="10%" stop-color="white" />
      <stop offset="30%" stop-color="#87CEEB" />
      <stop offset="100%" stop-color="azure" />
    </radialGradient>
  </defs>

  <marker
    id="arrowhead"
    markerWidth="8"
    markerHeight="6"
    refX="8"
    refY="3"
    orient="auto"
  >
    <polygon points="0 0, 8 3, 0 6" style="stroke-width: 0" />
  </marker>
  <marker
    id="treearrow"
    class="parent-child"
    markerWidth="4"
    markerHeight="3"
    refX="3"
    refY="1.5"
    orient="auto"
  >
    <polygon points="0 0, 4 1.5, 0 3" style="stroke-width: 0" />
  </marker>
</template>
