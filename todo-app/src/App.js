import './App.css';
import TodoTemplate from "./component/TodoTemplate";
import TodoInsert from "./component/TodoInsert";

function App() {
  return (
    <TodoTemplate>
      <TodoInsert />
    </TodoTemplate>
  );
}

export default App;
