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
      </ul>
      <Routes>
        <Route path="/" element={<Home/>}/>
        <Route path="/about" element={<About/>}/>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
