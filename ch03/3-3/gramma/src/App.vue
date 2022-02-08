<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <watch-watch-effect />
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import WatchWatchEffect from "./components/WatchWatchEffect.vue";

export default {
  name: 'App',
  components: {
    HelloWorld,
    WatchWatchEffect
  },
}
</script>
