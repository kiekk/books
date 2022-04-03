import { Component } from 'react';
import PropTypes from 'prop-types';

class MyComponent extends Component {
  render() {
    const { name, favoriteNumber, children } = this.props;
    return (
      <div>
        My First Component, prop: {name}<br/>
        children name is {children}
        <br />
        My FavoriteNumber is {favoriteNumber}
      </div>
    )
  }
}

MyComponent.defaultProps = {
  name: 'Default Props Name, React'
}

MyComponent.propTypes = {
  name: PropTypes.string,
  favoriteNumber: PropTypes.number.isRequired
}

export default MyComponent;