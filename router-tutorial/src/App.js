import {Routes, Route, BrowserRouter, Link} from 'react-router-dom';
import About from "./About";
import Home from "./Home";
import Profile from "./Profile";

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
        <li>
          <Link to="/profile/testUser">
            testUser 프로필
          </Link>
        </li>
        <li>
          <Link to="/profile/gildong">
            홍길동 프로필
          </Link>
        </li>
      </ul>
      <Routes>
        <Route path="/" element={<Home/>}/>
        <Route path="/about" element={<About/>}/>
        <Route path="/info" element={<About/>}/>
        <Route path="/profile/:username" element={<Profile/>}/>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
