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
        <Route path="/profiles" elenemt={<div>사용자를 선택해 주세요.</div>}/>
        <Route path=":username" element={<Profile/>}/>
      </Routes>
    </div>
  )
}

export default Profiles;