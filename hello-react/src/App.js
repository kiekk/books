import './App.css';
import { Component } from 'react';

class App extends Component {
  render() {
    const name = 'react';
    return <div className="react">{name}</div>;
  }
}

// 컴포넌트를 선언하는 방법 - 2 (클래스형 컴포넌트)
export default App;
