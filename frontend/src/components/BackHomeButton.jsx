import { Link } from 'react-router-dom';

export default function BackHomeButton() {
  return (
    <Link to="/" className="link-btn subtle-link">
      Back to Home
    </Link>
  );
}
