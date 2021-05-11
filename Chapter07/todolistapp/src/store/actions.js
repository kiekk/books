import Constant from '../Constant'

export default {
    [Constant.ADD_TODO]: ({ commit }, payload) => {
        console.log("###addTodo", payload)
        commit(Constant.ADD_TODO, payload)
    },
    [Constant.DELETE_TODO]: ({ commit }, payload) => {
        console.log("###deleteTodo", payload)
        commit(Constant.DELETE_TODO, payload)
    },
    [Constant.DONE_TOGGLE]: ({ commit }, payload) => {
        console.log("###doneToggle", payload)
        commit(Constant.DONE_TOGGLE, payload)
    }
}