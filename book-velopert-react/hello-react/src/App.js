// import MyComponent from './MyComponent'
// import Counter from './Counter'
// import Say from './Say'
// import EventPractice from "./EventPractice";
// import ValidationSample from './ValidationSample'
import { Component } from 'react'
// import ScrollBox from "./ScrollBox";
// import IterationSample from "./IterationSample";
import LikeCycleSample from "./LikeCycleSample";
import ErrorBoundary from "./ErrorBoundary";

// 랜덤 색상을 생성합니다.
function getRandomColor() {
    return '#' + Math.floor(Math.random() * 16777215).toString(16)
}

class App extends Component {
    state = {
        color: '#000000'
    }

    handleClick = () => {
        this.setState({
            color: getRandomColor()
        })
    }

    render() {
        return (
            <>
                <button onClick={this.handleClick}>랜덤 생성</button>
                <ErrorBoundary>
                    <LikeCycleSample color={this.state.color} />
                </ErrorBoundary>
            </>
        )
    }
}

export default App;