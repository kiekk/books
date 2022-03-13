import './App.css';

function App() {
  const name = 'React';
  /* -이 포함되는 스타일은 - 대신 카멜표기법으로 작성해야 합니다
    ex)
    background-color -> backgroundColor
    font-size -> fontSize
    font-weight -> fontWeight
   */

  const style = {
    backgroundColor: 'black',
    color: 'aqua',
    fontSize: '48px',
    fontWeight: 'bold',
    padding: 16
  }
  return (
    <div style={style}>
      {name}
    </div>
  )
}

/*
  JSX 내부에서도 undefined일 경우 보여줄 값을 설정할 수 있다.
 */
export default App;
