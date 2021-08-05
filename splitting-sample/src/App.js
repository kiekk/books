import React, { useState, Suspense } from 'react'
import logo from './logo.svg'
import './App.css'

const SplitMe = React.lazy(() => import('./SplitMe'))

const App = () => {
  const [visible, setVisible] = useState(false)

  const onClick = () => {
    setVisible(true)
  }

  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} alt="logo" className="App-logo" />
        <p onClick={onClick}>Hello React!</p>
        <Suspense fallback={<div>loading...</div>}>
          {visible && <SplitMe />}
        </Suspense>
      </header>
    </div>
  )
}

export default App
