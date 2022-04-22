import React, {useState, Suspense} from "react";
import './App.css';
import logo from './logo.svg';

const SplitMe = React.lazy(() => import('./SplitMe'))

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
        <Suspense fallback={<div>loading...</div>}>
          {visible && <SplitMe />}
        </Suspense>
      </header>
    </div>
  )
}

export default App;
