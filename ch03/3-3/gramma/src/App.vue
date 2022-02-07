<template>
  <img alt="Vue logo" src="./assets/logo.png" />
  <HelloWorld msg="Hello Vue 3.0 + Vite" />
  <event-listener/>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import EventListener  from "./components/EventListener.vue";

export default {
  name: 'App',
  components: {
    HelloWorld,
    EventListener
  },
}
</script>
