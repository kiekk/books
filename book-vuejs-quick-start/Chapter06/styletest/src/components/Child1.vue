<template>
    <div class="main test">
        {{msg}}
        <child11 />
        {{msg}}
    </div>
</template>

<script>
import Child11 from './Child11.vue'

export default {
    name: 'child1',
    components: {Child11},
    data() {
        return {
            msg: 'Child1'
        }
    }
}
</script>

<style scoped>
    .main {
        border: 1px solid black;
        background-color: yellow;
        text-align: center;
    }
    .test {
        padding: 10px;
        text-decoration: underline;
        border: 1px solid black;
    }
</style>