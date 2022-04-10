import {Component} from "react";

class EventPractice extends Component {
  render() {
    return (
      <div>
        <h1>이벤트 연습</h1>
        <input type="text" name="message" onChange={(e) => {
          console.log(e);
        }}/>
      </div>
    )
  }
}

export default EventPractice;