import React, {useState} from "react";
import './App.css';
import logo from './logo.svg';
import loadable from '@loadable/component'

const SplitMe = loadable(() => import('./SplitMe'), {
  fallback: <div>loading...</div>
})

function App() {
  const [visible, setVisible] = useState(false)

  const onClick = () => {
    setVisible(true)
  }

  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo"/>
        <p onClick={onClick}>Hello React!</p>
        {visible && <SplitMe />}
      </header>
    </div>
  )
}

export default App;
