import {Component} from "react";
import './App.css';
import ValidationSample from "./ValidationSample";

class App extends Component {
  render() {
    return (
      <ValidationSample />
    )
  }
}

// 추후 ref를 사용할 예정이기 때문에 미리 class component로 변경
export default App;
