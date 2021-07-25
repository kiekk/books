import logo from './logo.svg';
import './App.css';
import { Component } from 'react'

// 1. 함수형 컴포넌트
// function App() {
//     const name = '리액트';
//     return (
//         <>
//             <div className="react">{name}</div>
//             <input />
//         </>
//     )
// }

// 2. 클래스형 컴포넌트
class App extends Component {
    render() {
        const name = 'React';
        return (
            <div className="react">{name}</div>
        );
    }
}

/*
    클래스형 컴포넌트에서는 render 함수가 꼭 있어야 하며, render 함수에서 JSX를 반환해야 합니다.
 */

export default App;