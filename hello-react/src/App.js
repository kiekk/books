import './App.css';

function App() {
  const name = 'React';
  /*
    style을 미리 선언하지 않고 적용할 경우에는 아래와 같이 직접 값을 입력합니다.
   */

  return (
    <>
      <div className="react">{name}</div>
      {/*태그를 꼭 닫아준다*/}
      <input/>
    </>
  )
}
export default App;
