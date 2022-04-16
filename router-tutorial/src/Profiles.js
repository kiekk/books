import Profile from "./Profile";
import {Link, Route, Routes} from "react-router-dom";

const Profiles = () => {
  return (
    <div>
      <h3>사용자 목록:</h3>
      <ul>
        <li>
          <Link to="/profiles/testUser">testUser</Link>
        </li>
        <li>
          <Link to="/profiles/gildong">gildong</Link>
        </li>
      </ul>

      <Routes>
        <Route path="/profiles" render={() => <div>사용자를 선택해 주세요.</div>}/>
        {/*
          v6부터는 아래 route 가 제대로 동작하지 않습니다.
        */}
        <Route path="/profiles/:username" element={<Profile/>}/>
      </Routes>
    </div>
  )
}

export default Profiles;