import {Component} from "react";

class EventPractice extends Component {

  state = {
    username: '',
    message: ''
  }

  constructor(props) {
    super(props);
  }

  handleChange = (e) => {
    this.setState({
      [e.target.name]: e.target.value
    })
  }

  handleClick = () => {
    this.setState({
      username: '',
      message: '',
    })
  }

  render() {
    return (
      <div>
        <h1>이벤트 연습</h1>
        <input type="text" name="username" value={this.state.username} onChange={this.handleChange}/>
        <input type="text" name="message" value={this.state.message} onChange={this.handleChange}/>
        <h2>{this.state.username}: {this.state.message}</h2>
        <button onClick={this.handleClick}>
          Reset
        </button>
      </div>
    )
  }
}

export default EventPractice;