<template>
  <div id="talkweb-root">
    <MainView v-if="authStore.isAuthenticated" />
    <LoginView v-else />
  </div>
</template>

<script setup>
import { useAuthStore } from './stores/authStore';
import LoginView from './views/LoginView.vue';
import MainView from './views/MainView.vue';

const authStore = useAuthStore();
</script>

<style>
#talkweb-root {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}
</style>
