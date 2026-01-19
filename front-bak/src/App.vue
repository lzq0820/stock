<template>
  <div id="app">
    <MainLayout />
  </div>
</template>

<script>
import MainLayout from './layouts/MainLayout.vue';

export default {
  name: 'App',
  components: {
    MainLayout
  }
}
</script>

<style>
#app {
  height: 100vh;
  overflow: hidden;
  margin: 0;
  padding: 0;
}
</style>