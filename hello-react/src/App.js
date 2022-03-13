import './App.css';

function App() {
  const name = 'React';
  /*
    style을 미리 선언하지 않고 적용할 경우에는 아래와 같이 직접 값을 입력합니다.
   */

  return (
    <div style={{
      backgroundColor: 'black',
      color: 'aqua',
      fontSize: '48px',
      fontWeight: 'bold',
      padding: 16
    }}>
      {name}
    </div>
  )
}

/*
  JSX 내부에서도 undefined일 경우 보여줄 값을 설정할 수 있다.
 */
export default App;
