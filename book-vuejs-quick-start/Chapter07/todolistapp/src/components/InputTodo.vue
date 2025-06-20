<style>
    * {
        box-sizing: border-box;
    }
    .input {
        border: none;
        width: 75%;
        height: 35px;
        padding: 10px;
        float: left;
        font-size: 16px;
    }
    
    .addbutton {
        padding: 10px;
        width: 25%;
        height: 35px;
        background: #d9d9d9;
        color: #555;
        font-size: 13px;
        cursor: pointer;
        transition: 0.3s;
        float: left;
        text-align: center;
    }
    
    .addbutton:hover {
        background-color: #bbb;
    }
</style>

<template>
    <div>
        <input type="text" class="input" id="task" v-model.trim="todo" placeholder="입력 후 Enter" @keyup.enter="addTodo">
        <span class="addbutton" @click="addTodo">추가</span>
    </div>
</template>

<script>
import Constant from '../Constant'

export default {
    name: 'input-todo',
    data: function(){
        return {
            todo: ""
        }
    },
    methods: {
        addTodo: function(){
            this.$store.dispatch(Constant.ADD_TODO, {todo: this.todo})
            this.todo = ''
        }
    }
}
</script>
