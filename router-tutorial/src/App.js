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
        <Route path={["/about", "/info"]} element={<About/>}/>
      {/*
          리액트 v6 이후에는 이렇게 path 를 배열로 작성하게 될 경우 에러 발생
          Uncaught TypeError: meta.relativePath.startsWith is not a function
          at router.ts:193:1
          at Array.forEach (<anonymous>)
          at flattenRoutes (router.ts:185:1)
          at matchRoutes (router.ts:155:1)
          at useRoutes (hooks.tsx:345:1)
          at Routes (components.tsx:256:1)
          at renderWithHooks (react-dom.development.js:16141:1)
          at mountIndeterminateComponent (react-dom.development.js:20838:1)
          at beginWork (react-dom.development.js:22342:1)
          at beginWork$1 (react-dom.development.js:27219:1)
      */}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
