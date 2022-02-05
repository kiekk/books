<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <DeclarativeRendering/>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import DeclarativeRendering from "./components/DeclarativeRendering.vue";

export default {
  name: 'App',
  components: {
    DeclarativeRendering,
    HelloWorld
  },
}
</script>
