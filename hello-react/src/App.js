import logo from './logo.svg';
import './App.css';

function App() {
  return (
    <h1>Hello React!</h1>
    <h1>is it work?</h1>
  );
}
/*
  Error 발생
  하나의 컴포넌트에 여러 요소가 있다면 반드시 부모 요소 하나로 감싸야 합니다.
  SyntaxError: E:\study\study-react-remind\hello-react\src\App.js: Adjacent JSX elements must be wrapped in an enclosing tag. Did you want a JSX fragment <>...</>? (7:4)

   5 |   return (
   6 |     <h1>Hello React!</h1>
>  7 |     <h1>is it work?</h1>
     |     ^
   8 |   );
   9 | }

 */
export default App;
