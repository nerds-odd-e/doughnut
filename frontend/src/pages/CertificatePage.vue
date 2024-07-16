<template>
  <div class="certificate-frame">
    <div class="certificate-container">
      <span>This to certificate that</span>
      <span class="reciever-name">Korn</span>
      <p class="certificate-detail"><span>by completing the qualifications, </span><span>is granted the Certified Vue Expert</span></p>
      <div class="date-container">
        <span>on</span>
        <span class="date">2015-07-31</span>
        <span>, and expiring on</span>
        <span class="date">2015-07-31</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style lang="scss" scoped>
.certificate-frame {
  border: solid 16px lightskyblue;
  margin: 32px 64px;
}
.certificate-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 80vh;
  row-gap: 32px;
  border: solid 2px lightskyblue;
  margin: 8px;
}
.certificate-detail {
  display: flex;
  flex-direction: column;
  text-align: center;
}
.reciever-name{
  font-size: 32px;
  font-weight: 700;
  color: lightskyblue;
}
.date-container {
  display: flex;
  column-gap: 4px;
}
.date {
  color: lightskyblue;
  text-decoration: underline;
}
</style>