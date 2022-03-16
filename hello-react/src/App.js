import './App.css';

function App() {
  const name = 'React';
  /*
    style을 미리 선언하지 않고 적용할 경우에는 아래와 같이 직접 값을 입력합니다.
   */

  return (
    <>
      <div className="react">{name}</div>
      <input>
    </>
  )
}

/*
      태그를 닫지 않을 경우 아래와 같은 에러 발생
      ERROR in ./src/App.js
      Module build failed (from ./node_modules/babel-loader/lib/index.js):
      SyntaxError: E:\study\study-react-remind\hello-react\src\App.js: Unterminated JSX contents. (13:7)

      11 |       <div className="react">{name}</div>
      12 |       <input>
      > 13 |     </>
      |        ^
      14 |   )
      15 | }

      */
export default App;
