const MyComponent = props => {
  return (
    <div>
      My First Component, prop: {props.name}<br/>
      children name is {props.children}
    </div>
  );
}

MyComponent.defaultProps = {
  name: 'Default Props Name, React'
}

export default MyComponent;