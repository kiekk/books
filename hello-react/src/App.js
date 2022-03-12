import './App.css';

function App() {
  const name = undefined;
  return (
    <div>
      {name || '값이 undefined 입니다.'}
    </div>
  )
}

/*
  JSX 내부에서도 undefined일 경우 보여줄 값을 설정할 수 있다.
 */
export default App;
