import PropTypes from 'prop-types'

const MyComponent = ({ name, favoriteNumber, children }) => {
    return (
        <div>
            My First Component!<br />
            My Name is {name}<br />
            children은 {children}입니다.<br />
            My FavoriteNumber is {favoriteNumber}
        </div>
    )
}

MyComponent.defaultProps = {
    name: '김수박'
}

// name은 무조건 String으로 전달해야 함
MyComponent.propTypes = {
    name: PropTypes.string,
    favoriteNumber: PropTypes.number.isRequired
}

export default MyComponent;