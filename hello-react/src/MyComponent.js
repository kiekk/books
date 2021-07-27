import { Component } from 'react'
import PropTypes from 'prop-types'

// 함수형 컴포넌트
// const MyComponent = ({ name, favoriteNumber, children }) => {
//     return (
//         <div>
//             My First Component!<br />
//             My Name is {name}<br />
//             children은 {children}입니다.<br />
//             My FavoriteNumber is {favoriteNumber}
//         </div>
//     )
// }

class MyComponent extends Component {
    // 클래스형 컴포넌트에서 defaultProps, propTypes 설정
    static defaultProps = {
        name: '김수박'
    }
    static propTypes = {
        name: PropTypes.string,
        favoriteNumber: PropTypes.number.isRequired
    }
    render() {
        const { name, favoriteNumber, children } = this.props
        return (
            <div>
                My First Component!<br />
                My Name is {name}<br />
                children은 {children}입니다.<br />
                My FavoriteNumber is {favoriteNumber}
            </div>
        )
    }
}

// MyComponent.defaultProps = {
//     name: '김수박'
// }
//
// // name은 무조건 String으로 전달해야 함
// MyComponent.propTypes = {
//     name: PropTypes.string,
//     favoriteNumber: PropTypes.number.isRequired
// }

/*
    PropType 종류
    
    ㆍ array: 배열
    ㆍ arrayOf(다른 PropType): 특정 PropType으로 이루어진 배열을 의미합니다.
       ex) arrayOf(PropTypes.number)는 숫자로 이루어진 배열입니다.
    ㆍ bool: true or false
    ㆍ func: 함수
    ㆍ object: 객체
    ㆍ string: 문자열
    ㆍ symbol: ES6의 Symbol
    ㆍ node: 렌더링 할 수 있는 모든 것(숫자, 문자열, JSX 코드)
    ㆍ instanceOf(클래스): 특정 클래스의 인스턴스
       ex) instanceOf(MyClass)
    ㆍ oneOf(['dog', 'cat']): 주어진 배열 요소 값 중 하나
    ㆍ oneOfType([React.PropTypes.string, PropTypes.number]): 주어진 배열 안의 종류 중 하나
    ㆍ objectOf(React.PropTypes.number): 객체의 모든 키 값이 인자로 주어진 PropTypes인 객체
    ㆍ shape({ name: PropTypes.string, num: PropTypes.number }): 주어진 스키마를 가진 객체
    ㆍ any: 아무 종류
 */
export default MyComponent;