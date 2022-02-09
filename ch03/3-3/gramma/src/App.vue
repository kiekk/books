<template>
  <img alt="Vue logo" src="./assets/logo.png"/>
  <HelloWorld msg="Hello Vue 3.0 + Vite"/>
  <provide-inject />
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import ProvideInject from "./components/ProvideInject.vue";

export default {
  name: 'App',
  components: {
    ProvideInject,
    HelloWorld,
  },
  provide: {
    msg: '메시지',
  },
}
</script>
