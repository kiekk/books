import { useReducer, useRef, useCallback } from 'react'
import TodoTemplate from "./components/TodoTemplate";
import TodoInsert from './components/TodoInsert'
import TodoList from "./components/TodoList";

function createBulkTodos() {
    const array = []
    for(let i=1;i<=2500;i++){
        array.push({
            id: i,
            text: `할일 ${i}`,
            checked: false,
        })
    }
    return array
}

function todoReducer(todos, action) {
    switch(action.type) {
        case 'INSERT' : // 새로 추가
            // { type: 'INSERT', todo: { id: 1, text: 'todo', checked: false } }
            return todos.concat(action.todo)
        case 'REMOVE': // 제거
            // { type: 'REMOVE', id: 1 }
            return todos.filter(todo => todo.id !== action.id)
        case 'TOGGLE': // 토글
            // { type: 'TOGGLE', id: 1 }
            return todos.map(todo => todo.id === action.id ? { ...todo, checked: !todo.checked } : todo)
        default:
            return todos

    }
}

const App = () => {
    // useReducer 두 번째 파라미터에는 초기 상태를 넣어줘야 합니다.
    // 지금처럼 세 번째 파라미터에 초기 상태를 넣어주면, 컴포넌트가 처음 렌더링 될 때만
    // createBulkTodos가 호출됩니다.
    const [todos, dispatch] = useReducer(todoReducer, undefined, createBulkTodos)

    const nextId = useRef(2501)

    const onInsert = useCallback(
        text => {
            const todo = {
                id: nextId.current,
                text,
                checked: false,
            }
            dispatch({ type: 'INSERT', todo})
            nextId.current += 1
        },
        [],
    )

    const onRemove = useCallback(
        id => {
            dispatch( { type: 'REMOVE', id } )
        },
        [],
    )

    const onToggle = useCallback(
        id => {
            dispatch( { type: 'TOGGLE', id })
        },
        [],
    )

    return (
        <TodoTemplate>
            <TodoInsert onInsert={onInsert}/>
            <TodoList todos={todos} onRemove={onRemove} onToggle={onToggle}/>
        </TodoTemplate>
    )
}

export default App;
