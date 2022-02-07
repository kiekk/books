<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <DeclarativeRendering/>
  <v-html/>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import DeclarativeRendering from "./components/DeclarativeRendering.vue";
import VHtml from "./components/v-html.vue";

export default {
  name: 'App',
  components: {
    VHtml,
    DeclarativeRendering,
    HelloWorld
  },
}
</script>
