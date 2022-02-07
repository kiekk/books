<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <DeclarativeRendering/>
  <v-html/>
  <v-pre/>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import DeclarativeRendering from "./components/DeclarativeRendering.vue";
import VHtml from "./components/v-html.vue";
import VPre from "./components/v-pre.vue";

export default {
  name: 'App',
  components: {
    VPre,
    VHtml,
    DeclarativeRendering,
    HelloWorld
  },
}
</script>
