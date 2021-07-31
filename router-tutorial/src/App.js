import React from 'react'
import { Route } from 'react-router-dom'
import About from './About'
import Home from './Home'

// '/about' 접속 시 '/' 규칙도 일치하기 때문에 Home, About 컴포넌트가 같이 출력
// Home 컴포넌트에 exact=true 적용하여 '/about' 접속 시 About 컴포넌트만 출력
const App = () => {
    return (
        <div>
            <Route path="/" component={Home} exact={true}/>
            <Route path="/about" component={About} />
        </div>
    )
}

export default App