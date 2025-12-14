import './App.css';
import OrchestratorForm from './components/OrchestratorForm';

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <div className="logo-container">
          <h1>⚡ Intelligent Test Data Generator</h1>
        </div>
      </header>
      <main className="App-main">
        <OrchestratorForm />
      </main>
      <footer className="App-footer">
        <p>© 2025 ITDG Project. Powered by AI Agent.</p>
      </footer>
    </div>
  );
}

export default App;
