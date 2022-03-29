const MyComponent = props => {
  const { name, children } = props;
  return (
    <div>
      My First Component, prop: {name}<br/>
      children name is {children}
    </div>
  );
}

MyComponent.defaultProps = {
  name: 'Default Props Name, React'
}

export default MyComponent;