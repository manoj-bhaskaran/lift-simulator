import { Component } from 'react';

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Unhandled application error', error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.error) {
      return (
        <div className="app-error-boundary" role="alert">
          <div className="app-error-boundary__card">
            <h1>Something went wrong</h1>
            <p>
              The admin UI hit an unexpected error. Reload the page to try again,
              or return to the dashboard if the problem persists.
            </p>
            <pre className="app-error-boundary__details">
              {this.state.error.message || 'Unknown error'}
            </pre>
            <div className="app-error-boundary__actions">
              <button type="button" className="btn-primary" onClick={this.handleReload}>
                Reload page
              </button>
              <a className="btn-secondary" href="/">
                Go to dashboard
              </a>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
