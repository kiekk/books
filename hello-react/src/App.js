import './App.css';

function App() {
  const name = undefined;
  return name;
}
/*

함수에서 바로 undefined를 반환할 경우 아뢔와 같은 에러가 발생한다.
Uncaught Error: App(...): Nothing was returned from render. This usually means a return statement is missing. Or, to render nothing, return null.
 */
export default App;
