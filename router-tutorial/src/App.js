import React from 'react'
import { Route, Link } from 'react-router-dom'
import About from './About'
import Home from './Home'

/*
    Link 컴포넌트
    다른 주소로 이동시켜 주는 컴포넌트

    *리액트 라우터를 사용할 때는 a 태그를 직접 사용하게 되면
    페이지를 새로 불러오기 때문에 기존 상태들이 초기화 됩니다.

    Link 컴포넌트를 사용하여 페이지를 전환하면,
    페이지를 새로 불러오지 않고 애플리케이션은 그대로 유지한 상태에서
    페이지의 주소만 변경해 줍니다.
 */
const App = () => {
    return (
        <div>
            <ul>
                <li>
                    <Link to="/">홈</Link>
                </li>
                <li>
                    <Link to="/about">소개</Link>
                </li>
            </ul>
            <hr />
            <Route path="/" component={Home} exact={true} />
            <Route path={['/about', '/info']} component={About} />
        </div>
    )
}

export default App