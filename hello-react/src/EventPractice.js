import {Component} from "react";

class EventPractice extends Component {

  state = {
    message: ''
  }

  render() {
    return (
      <div>
        <h1>이벤트 연습</h1>
        <input type="text" name="message" value={this.state.message} onChange={(e) => {
          this.setState({message: e.target.value})
        }}/>
        <h2>{this.state.message}</h2>
        <button onClick={() => {
          this.setState({
            message: ''
          })
        }}>
          Reset
        </button>
      </div>
    )
  }
}

export default EventPractice;