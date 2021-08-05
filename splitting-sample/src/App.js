import React, { useState } from 'react'
import logo from './logo.svg'
import './App.css'
import loadable from '@loadable/component'

const SplitMe = loadable(() => import('./SplitMe'), {
  fallback: <div>loading...</div>,
})

const App = () => {
  const [visible, setVisible] = useState(false)

  const onClick = () => {
    setVisible(true)
  }

  const onMouseOver = () => {
    console.log('preload 호출')
    SplitMe.preload()
  }

  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} alt="logo" className="App-logo" />
        <p onClick={onClick} onMouseOver={onMouseOver}>
          Hello React!
        </p>
        {visible && <SplitMe />}
      </header>
    </div>
  )
}

export default App
