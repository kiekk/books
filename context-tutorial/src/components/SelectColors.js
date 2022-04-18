import {Component} from "react";

const colors = ['red', 'orange', 'yellow', 'green', 'blue', 'indigo', 'violet']

class SelectColors extends Component {
  render() {
    return (
      <div>
        <h2>색상을 입력하세요.</h2>
        <div style={{display: 'flex'}}>
          {colors.map(color => (
            <div
              key={color}
              style={{
                background: color,
                width: '24px',
                height: '24px',
                cursor: 'pointer'
              }}
            />
          ))}
        </div>
        <hr/>
      </div>
    )
  }
}

export default SelectColors;