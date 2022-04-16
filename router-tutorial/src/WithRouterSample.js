import {useLocation, useParams, useNavigate} from 'react-router-dom'

const WithRouterSample = () => {
  const params = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <div>
      <h4>location</h4>
      <textarea value={JSON.stringify(location, null, 2)} rows={7} readOnly={true} />
      <h4>match</h4>
      <textarea value={JSON.stringify(params)} rows={7} readOnly={true} />
      <button onClick={() => navigate('/')}>홈으로</button>
    </div>
  )
}

export default WithRouterSample;