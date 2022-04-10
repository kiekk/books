import {Component} from "react";

class EventPractice extends Component {

  state = {
    message: ''
  }

  constructor(props) {
    super(props);
    this.handleChange = this.handleChange.bind(this);
    this.handleClick = this.handleClick.bind(this);
  }

  handleChange(e) {
    this.setState({
      message: e.target.value
    })
  }

  handleClick() {
    this.setState({
      message: ''
    })
  }

  render() {
    return (
      <div>
        <h1>이벤트 연습</h1>
        <input type="text" name="message" value={this.state.message} onChange={this.handleChange}/>
        <h2>{this.state.message}</h2>
        <button onClick={this.handleClick}>
          Reset
        </button>
      </div>
    )
  }
}

export default EventPractice;