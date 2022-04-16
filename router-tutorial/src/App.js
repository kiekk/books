import {Routes, Route, BrowserRouter, Link} from 'react-router-dom';
import About from "./About";
import Home from "./Home";

function App() {
  return (
    <BrowserRouter>
      <ul>
        <li>
          <Link to="/">
            홈
          </Link>
        </li>
        <li>
          <Link to="/about">
            소개
          </Link>
        </li>
        <li>
          <Link to="/info">
            소개2
          </Link>
        </li>
      </ul>
      <Routes>
        <Route path="/" element={<Home/>}/>
        <Route path="/about" element={<About/>}/>
        <Route path="/info" element={<About/>}/>
      {/*  
        리액트 v5 이전에 사용했던 route 를 여러 번 사용하는 방법으로 해결
      */}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
