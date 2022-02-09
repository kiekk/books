import { createApp } from 'vue'
import App from './App.vue'
import './index.css'

const app = createApp(App);

app.directive('notification', (el, binding, vnode, prevNode) => {
  el.style.position = 'fixed'
  el.style[binding.arg || 'top'] = binding.value + 'px'
})

app.mount('#app')
