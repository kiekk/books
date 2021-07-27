import PropTypes from 'prop-types'

const MyComponent = ({ name, children }) => {
    return (
        <div>
            My First Component!<br />
            My Name is {name}<br />
            children은 {children}입니다.
        </div>
    )
}

MyComponent.defaultProps = {
    name: '김수박'
}

// name은 무조건 String으로 전달해야 함
MyComponent.propTypes = {
    name: PropTypes.string
}

export default MyComponent;