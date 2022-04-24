import React from 'react'
import UsersContainer from '../containers/UsersContainer'
import {Route} from "react-router-dom";

const UsersPage = () => {
  return (
    <>
      <UsersContainer/>
      <Route path="/users/:id" render={({match}) => <UsersContainer id={match.params.id}/>}/>
    </>
  )
}

export default UsersPage
