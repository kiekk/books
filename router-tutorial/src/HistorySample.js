import {Component} from "react";

class HistorySample extends Component {

  handleGoBack = () => {
    history.goBack();
  }

  handleGoHome = () => {
    history.push('/')
  }

  componentDidMount = () => {
    this.unblock = history.block('정말 떠나실 건가요?')
  }

  componentWillUnmount() {
    if (this.unblock) {
      this.unblock()
    }
  }


  render() {
    return (
      <div>
        <button onClick={this.handleGoBack}>뒤로</button>
        <button onClick={this.handleGoHome}>홈으로</button>
      </div>
    )
  }
}

export default HistorySample