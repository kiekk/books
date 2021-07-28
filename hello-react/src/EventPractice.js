import { Component } from 'react'
/*
    이벤트 사용시 주의 사항
    1. 이벤트 이름은 카멜 표기법으로 작성
    2. 이벤트에 코드가 아닌 함수 형태의 값을 전달
    3. DOM 요소에만 이벤트 설정 가능
 */
class EventPractice extends Component {
    state = {
        username: '',
        message: '',
    }

    handleChange = (e) => {
        this.setState({
            [e.target.name]: e.target.value
        })
    }

    handleClick = () => {
        alert(this.state.username + ': ' + this.state.message)
        this.setState({
            username: '',
            message: ''
        })
    }

    handleKeyPress = (e) => {
        if(e.key === 'Enter') {
            this.handleClick()
        }
    }

    render() {
        return (
            <div>
                <h1>이벤트 연습</h1>
                <h2>{this.state.message}</h2>
                <input
                    type="text"
                    name="username"
                    placeholder="사용자명"
                    value={this.state.username}
                    onChange={this.handleChange}
                />
                <input
                    type="text"
                    name="message"
                    placeholder="아무거나 입력해 보세요"
                    value={this.state.message}
                    onChange={this.handleChange}
                    onKeyPress={this.handleKeyPress}/>
                <button
                    onClick={this.handleClick}
                >
                    확인
                </button>
            </div>
        )
    }
}

export default EventPractice