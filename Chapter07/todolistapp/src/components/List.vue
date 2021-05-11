<style>
    * {
        box-sizing: border-box;
    }
        ul {
        margin: 0;
        padding: 0;
    }
    
    ul li {
        cursor: pointer;
        position: relative;
        padding: 8px 8px 8px 40px;
        user-select: none;
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
    }
    
    ul li:hover {
        background-color: #ddd;
    }
    
    ul li.checked {
        background: #bbb;
        color: #fff;
        text-decoration: line-through;
    }
    
    ul li.checked::before {
        content: "";
        position: absolute;
        border-color: #fff;
        border-style: solid;
        border-width: 0px 1px 1px 0px;
        top: 10px;
        left: 16px;
        transform: rotate(45deg);
        height: 8px;
        width: 8px;
    }
    
    .close {
        position: absolute;
        right: 0;
        top: 0;
        padding: 12px 16px;
    }
    
    .close:hover {
        background-color: #f44336;
        color: white;
    }
</style>

<template>
    <ul id="todolist">
        <li v-for="a in todolist" :class="checked(a.done)" @click="doneToggle(a.id)" :key="a.id">
            <span>{{a.todo}}</span>
            <span v-if="a.done"> (완료)</span>
            <span class="close" @click.stop="deleteTodo(a.id)">&#x00D7;</span>
        </li>
    </ul>
</template>

<script>
import Constant from '../Constant'

export default {
    name: 'List',
    computed: {
        todolist() {
            return this.$store.state.todolist;
        }
    },
    methods: {
        checked: function(done){
            if(done) {
                return {
                    checked: true
                }
            }else {
                return {
                    checked: false
                }
            }
        },
        doneToggle: function(id){
            this.$store.commit(Constant.DONE_TOGGLE, {id:id})
        },
        deleteTodo: function(id){
            this.$store.commit(Constant.DELETE_TODO, {id:id})
        }
    }
}
</script>