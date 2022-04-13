import './App.css';
import TodoTemplate from "./component/TodoTemplate";
import TodoInsert from "./component/TodoInsert";
import TodoList from "./component/TodoList";

function App() {
  return (
    <TodoTemplate>
      <TodoInsert />
      <TodoList />
    </TodoTemplate>
  );
}

export default App;
