<template>
  <div class="d-flex sidebar-control">
    <div v-if="user" class="sidebar-container">
        <ul class="list-group">
          <li role="button" class="list-item" title="Daily Review">
            <router-link :to="{ name: 'reviews' }" class="d-flex flex-column align-items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-calendar-check" viewBox="0 0 16 16">
                <path d="M10.854 7.146a.5.5 0 0 1 0 .708l-3 3a.5.5 0 0 1-.708 0l-1.5-1.5a.5.5 0 1 1 .708-.708L7.5 9.793l2.646-2.647a.5.5 0 0 1 .708 0z"/>
                <path d="M3.5 0a.5.5 0 0 1 .5.5V1h8V.5a.5.5 0 0 1 1 0V1h1a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V3a2 2 0 0 1 2-2h1V.5a.5.5 0 0 1 .5-.5M1 4v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V4z"/>
              </svg>
              <span class="menu-label">Daily Review</span>
            </router-link>
          </li>
          <li role="button" class="list-item" title="My Notebooks">
            <router-link :to="{ name: 'notebooks' }" class="d-flex flex-column align-items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-journal-text" viewBox="0 0 16 16">
                <path d="M5 10.5a.5.5 0 0 1 .5-.5h2a.5.5 0 0 1 0 1h-2a.5.5 0 0 1-.5-.5m0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5m0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5m0-2a.5.5 0 0 1 .5-.5h5a.5.5 0 0 1 0 1h-5a.5.5 0 0 1-.5-.5"/>
                <path d="M3 0h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2v-1h1v1a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1H3a1 1 0 0 0-1 1v1H1V2a2 2 0 0 1 2-2"/>
                <path d="M1 5v-.5a.5.5 0 0 1 1 0V5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1zm0 3v-.5a.5.5 0 0 1 1 0V8h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1zm0 3v-.5a.5.5 0 0 1 1 0v.5h.5a.5.5 0 0 1 0 1h-2a.5.5 0 0 1 0-1z"/>
              </svg>
              <span class="menu-label">Notebooks</span>
            </router-link>
          </li>
          <li role="button" class="list-item" title="Recent">
            <router-link :to="{ name: 'recent' }" class="d-flex flex-column align-items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-clock-history" viewBox="0 0 16 16">
                <path d="M8.515 1.019A7 7 0 0 0 8 1V0a8 8 0 0 1 .589.022zm2.004.45a7 7 0 0 0-.985-.299l.219-.976c.383.086.76.2 1.126.342zm1.37.71a7 7 0 0 0-.439-.27l.493-.87a8 8 0 0 1 .979.654l-.615.789a7 7 0 0 0-.418-.302zm1.834 1.79a7 7 0 0 0-.653-.796l.724-.69c.27.285.52.59.747.91l-.818.576zm.744 1.352a7 7 0 0 0-.214-.468l.893-.45a8 8 0 0 1 .45 1.088l-.95.313a7 7 0 0 0-.179-.483m.53 2.507a7 7 0 0 0-.1-1.025l.985-.17c.067.386.106.778.116 1.17l-1 .025zm-.131 1.538c.033-.17.06-.339.081-.51l.993.123a8 8 0 0 1-.23 1.155l-.964-.267c.046-.165.086-.332.12-.501m-.952 2.379c.184-.29.346-.594.486-.908l.914.405c-.16.36-.345.706-.555 1.038l-.845-.535m-.964 1.205c.122-.122.239-.248.35-.378l.758.653a8 8 0 0 1-.401.432l-.707-.707z"/>
                <path d="M8 1a7 7 0 1 0 4.95 11.95l.707.707A8.001 8.001 0 1 1 8 0z"/>
                <path d="M7.5 3a.5.5 0 0 1 .5.5v5.21l3.248 1.856a.5.5 0 0 1-.496.868l-3.5-2A.5.5 0 0 1 7 9V3.5a.5.5 0 0 1 .5-.5"/>
              </svg>
              <span class="menu-label">Recent</span>
            </router-link>
          </li>
          <li role="button" class="list-item" title="Bazaar">
            <router-link :to="{ name: 'bazaar' }" class="d-flex flex-column align-items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-shop" viewBox="0 0 16 16">
                <path d="M2.97 1.35A1 1 0 0 1 3.73 1h8.54a1 1 0 0 1 .76.35l2.609 3.044A1.5 1.5 0 0 1 16 5.37v.255a2.375 2.375 0 0 1-4.25 1.458A2.371 2.371 0 0 1 9.875 8 2.37 2.37 0 0 1 8 7.083 2.37 2.37 0 0 1 6.125 8a2.37 2.37 0 0 1-1.875-.917A2.375 2.375 0 0 1 0 5.625V5.37a1.5 1.5 0 0 1 .361-.976l2.61-3.045zm1.78 4.275a1.375 1.375 0 0 0 2.75 0 .5.5 0 0 1 1 0 1.375 1.375 0 0 0 2.75 0 .5.5 0 0 1 1 0 1.375 1.375 0 1 0 2.75 0V5.37a.5.5 0 0 0-.12-.325L12.27 2H3.73L1.12 5.045A.5.5 0 0 0 1 5.37v.255a1.375 1.375 0 0 0 2.75 0 .5.5 0 0 1 1 0M1.5 8.5A.5.5 0 0 1 2 9v6h1v-5a1 1 0 0 1 1-1h3a1 1 0 0 1 1 1v5h6V9a.5.5 0 0 1 1 0v6h.5a.5.5 0 0 1 0 1H.5a.5.5 0 0 1 0-1H1V9a.5.5 0 0 1 .5-.5M4 15h3v-5H4zm5-5a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1zm3 0h-2v3h2z"/>
              </svg>
              <span class="menu-label">Bazaar</span>
            </router-link>
          </li>
          <li role="button" class="list-item" title="Circles">
            <router-link :to="{ name: 'circles' }" class="d-flex flex-column align-items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-people" viewBox="0 0 16 16">
                <path d="M15 14s1 0 1-1-1-4-5-4-5 3-5 4 1 1 1 1zm-7.978-1L7 12.996c.001-.264.167-1.03.76-1.72C8.312 10.629 9.282 10 11 10c1.717 0 2.687.63 3.24 1.276.593.69.758 1.457.76 1.72l-.008.002-.014.002zM11 7a2 2 0 1 0 0-4 2 2 0 0 0 0 4m3-2a3 3 0 1 1-6 0 3 3 0 0 1 6 0M6.936 9.28a6 6 0 0 0-1.23-.247A7 7 0 0 0 5 9c-4 0-5 3-5 4q0 1 1 1h4.216A2.24 2.24 0 0 1 5 13c0-1.01.377-2.042 1.09-2.904.243-.294.526-.569.846-.816M4.92 10A5.5 5.5 0 0 0 4 13H1c0-.26.164-1.03.76-1.724.545-.636 1.492-1.256 3.16-1.275ZM1.5 5.5a3 3 0 1 1 6 0 3 3 0 0 1-6 0m3-2a2 2 0 1 0 0 4 2 2 0 0 0 0-4"/>
              </svg>
              <span class="menu-label">Circles</span>
            </router-link>
          </li>
          <li role="button" class="list-item" title="Messages">
            <MessageCenterButton :user="user">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="currentColor" class="bi bi-chat" viewBox="0 0 16 16">
                <path d="M2.678 11.894a1 1 0 0 1 .287.801 10.97 10.97 0 0 1-.398 2c1.395-.323 2.247-.697 2.634-.893a1 1 0 0 1 .71-.074A8.06 8.06 0 0 0 8 14c3.996 0 7-2.807 7-6 0-3.192-3.004-6-7-6S1 4.808 1 8c0 1.468.617 2.83 1.678 3.894m-.493 3.905a21.682 21.682 0 0 1-.713.129c-.2.032-.352-.176-.273-.362a9.68 9.68 0 0 0 .244-.637l.003-.01c.248-.72.45-1.548.524-2.319C.743 11.37 0 9.76 0 8c0-3.866 3.582-7 8-7s8 3.134 8 7-3.582 7-8 7a9.06 9.06 0 0 1-2.347-.306c-.52.263-1.639.742-3.468 1.105z"/>
              </svg>
              <template #label>
                <span class="menu-label">Messages</span>
              </template>
            </MessageCenterButton>
          </li>
          <li role="button" class="list-item" title="User Actions">
            <UserActionsButton
              v-bind="{ user }"
              @update-user="$emit('updateUser', $event)"
            >
              <div class="d-flex flex-column align-items-center gap-1">
                <SvgMissingAvatar width="24" height="24" />
                <span class="menu-label">Account</span>
              </div>
            </UserActionsButton>
          </li>
        </ul>
      </div>
    <LoginButton v-else />
    <span class="vertical-text">Doughnut by</span>
    <a href="https://odd-e.com" target="_blank">
    <img
      src="/odd-e.png"
      width="35"
      height="35"
      class="d-inline-block align-top"
      alt=""
    />
  </a>
  </div>
</template>

<script setup lang="ts">
import type { User } from "@/generated/backend"
import type { PropType } from "vue"
import LoginButton from "@/components/toolbars/LoginButton.vue"
import UserActionsButton from "./UserActionsButton.vue"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"
import MessageCenterButton from "@/components/toolbars/MessageCenterButton.vue"

defineProps({
  user: { type: Object as PropType<User> },
})
defineEmits(["updateUser"])
</script>

<style lang="scss" scoped>
.sidebar-control {
  background-color: #f5f5f5;
  flex-direction: column;
  height: 100vh;
  align-items: center;
}

.sidebar-container {
  display: flex;
  flex-direction: column;
  height: auto;
  overflow: hidden;
  width: 100%;

  :deep(.dropup) {
    position: static;

    .dropdown-menu {
      position: fixed;
      bottom: 60px;
      left: 0;
    }
  }
}

.list-group {
  list-style: none;
  padding: 0;
  margin: 0;
}

.list-item {
  border: none;
  padding: 0.5rem;
  background: none;
  text-align: center;

  .menu-label {
    font-size: 0.7rem;
    line-height: 1;
  }
}

.vertical-text {
  writing-mode: vertical-lr;
  transform: none;
  text-align: center;
  padding: 1rem 0;
  font-weight: bold;
  color: #666;
  white-space: nowrap;
  margin-top: 1rem;
}

@media (max-width: 768px) {
  .sidebar-control {
    flex-direction: row;
    width: 100%;
    height: auto;
    padding: 0.5rem 0;
    align-items: center;
  }

  .sidebar-container {
    width: 100%;
    height: auto;
    display: block;

    .list-group {
      flex-direction: row;
      flex-wrap: nowrap;
      overflow-x: auto;
      justify-content: center;
      gap: 1rem;
    }

    .list-item {
      border: none;
      padding: 0.5rem;
      background: none;
    }

    .admin-dashboard {
      display: none;
    }

    .fixed-bottom-bar {
      display: none;
    }
  }

  .vertical-text,
  a[href="https://odd-e.com"] {
    display: none;
  }

  .list-item {
    .menu-label {
      font-size: 0.65rem;
    }
  }
}
</style>
