import { Component } from 'react';
import PropTypes from 'prop-types';

class MyComponent extends Component {
  static defaultProps = {
    name: 'Default Props Name, React'
  }

  static propTypes = {
    name: PropTypes.string,
    favoriteNumber: PropTypes.number.isRequired
  }

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

export default MyComponent;