<template>
  <router-link class="btn" :to="{ name: 'reviews' }">
    <svg class="doughnut-ring" viewBox="-50 -50 100 100" width="30" height="30">
      <path
        :d="`M0 -40,A40 40 0 0 1,20 34`"
        fill="none"
        stroke-width="20"
        stroke="#3baf3a"
      />
      <path
        :d="`M20 34,A40 40 0 1 1,0 -40`"
        fill="none"
        stroke-width="20"
        stroke="#3bafda"
      />
    </svg>
    Daily Review
  </router-link>
</template>
