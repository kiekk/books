import Vue from 'vue'
import App from './App.vue'
import store from './store'
import VueRouter from 'vue-router'
import Home from './components/Home'
import About from './components/About'
import ContactList from './components/ContactList'
import ContactForm from './components/ContactForm'
import UpdatePhoto from './components/UpdatePhoto'
import 'bootstrap/dist/css/bootstrap.css'
import ES6Promise from 'es6-promise'
ES6Promise.polyfill()

Vue.config.productionTip = false
Vue.use(VueRouter)

const router = new VueRouter({
    routes: [
        { path: '/', redirect: '/home' },
        { path: '/home', name: 'home', componen: Home },
        { path: '/about', name: 'about', component: About },
        {
            path: '/contacts',
            name: 'contacts',
            component: ContactList,
            children: [
                { path: 'add', name: 'addcontact', component: ContactForm },
                { path: 'update/:no', name: 'updatecontact', component: ContactForm, props: true },
                { path: 'photo:/:no', name: 'updatephoto', component: UpdatePhoto, props: true },
            ]
        },
    ]
})

new Vue({
    store,
    router,
    render: h => h(App),
}).$mount('#app')