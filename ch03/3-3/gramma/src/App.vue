<template>
  <img alt="Vue logo" src="./assets/logo.png"/>
  <HelloWorld msg="Hello Vue 3.0 + Vite"/>
  <slots>
    <template v-slot="slotProps"> ✔️ {{ slotProps.item }} </template>
  </slots>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import Slots from "./components/Slots.vue";

export default {
  name: 'App',
  components: {
    Slots,
    HelloWorld,
  },
}
</script>
