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

export default MyComponent;