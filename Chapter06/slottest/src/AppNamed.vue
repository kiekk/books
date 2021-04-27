<template>
    <div id="app">
        <layout>
            <h1 slot="header">헤더 영역</h1>

            <div slot="sidebar">
                <ul slot="sidebar">
                    <li v-for="sidebar in sidebars" :key="sidebar.menu">
                        <a :href="sidebar.link">{{sidebar.menu}}</a>
                    </li>
                </ul>
            </div>

            <div slot="content">
                <h2>컨텐츠 영역</h2>
                <p>Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ratione consequatur maxime tempore debitis quis obcaecati, aperiam expedita distinctio temporibus rerum veritatis. Rerum doloremque nobis asperiores quae fugiat ex quia. Incidunt?</p>
                <p>Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ratione consequatur maxime tempore debitis quis obcaecati, aperiam expedita distinctio temporibus rerum veritatis. Rerum doloremque nobis asperiores quae fugiat ex quia. Incidunt?</p>
                <p>Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ratione consequatur maxime tempore debitis quis obcaecati, aperiam expedita distinctio temporibus rerum veritatis. Rerum doloremque nobis asperiores quae fugiat ex quia. Incidunt?</p>
                <p>Lorem ipsum dolor sit, amet consectetur adipisicing elit. Ratione consequatur maxime tempore debitis quis obcaecati, aperiam expedita distinctio temporibus rerum veritatis. Rerum doloremque nobis asperiores quae fugiat ex quia. Incidunt?</p>
            </div>

            <p slot="footer">Footer text</p>
        </layout>
    </div>
</template>


<script>
    import Layout from './components/NamedSlot.vue'

    export default {
        data() {
            return {
                sidebars: [
                    {menu: "Homd", link: "#"},
                    {menu: "About", link: "#"},
                    {menu: "Contact", link: "#"},
                    {menu: "Vue.js", link: "#"}
                ]
            }
        },
        components: {Layout}
    }
</script>

<style scoped>
    ul.menu {
        position: relative;
        padding: 5px;
        list-style: none;
        font-style: italic;
    }
    ul.menu a {
        text-decoration: none;
    }
</style>
