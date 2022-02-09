<template>
  <img alt="Vue logo" src="./assets/logo.png"/>
  <HelloWorld msg="Hello Vue 3.0 + Vite"/>
  <p>{{ `[${msg}] : ${msg2}` }}</p>
  <customer-event v-model:msg="msg" v-model:msg2="msg2"/>
</template>

<script>
import HelloWorld from './components/HelloWorld.vue'
import CustomerEvent from "./components/CustomerEvent.vue";

export default {
  name: 'App',
  components: {
    CustomerEvent,
    HelloWorld,
  },
  data() {
    return {
      msg: '초기 메시지',
      msg2: '초기 메시지',
    }
  },
}
</script>
