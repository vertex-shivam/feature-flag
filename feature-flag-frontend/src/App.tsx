
import { useEffect, useState } from "react";
import "./App.css";

function App() {
    const [status, setStatus] = useState("Checking...");
    const [response, setResponse] = useState("");
    const [lastChecked, setLastChecked] = useState("");
    const [testAgain, setTestAgain] = useState(0);

    const health_endpoint = `${import.meta.env.VITE_BASE_URL}/api/health`;


    useEffect(() => {
        fetch(health_endpoint)
            .then((res) => {
                if (!res.ok) {
                    throw new Error("Backend error");
                }

                return res.json();
            })
            .then((data) => {
                setStatus("Connected");
                setResponse(JSON.stringify(data, null, 2));
                setLastChecked(new Date().toLocaleTimeString());
            })
            .catch(() => {
                setStatus("Disconnected");
                setResponse("Unable to connect to backend");
                setLastChecked(new Date().toLocaleTimeString());
            });
    }, [testAgain]);

    const isConnected = status === "Connected";
    const isChecking = status === "Checking...";

    return (
        <div className="app">
            <aside className="sidebar">
                <div className="logo">
                    <div className="logo-icon">F</div>
                    <span>Flagify</span>
                </div>

                <p className="sidebar-label">MENU</p>

                <div className="nav-item active">
                    <span>▦</span>
                    Overview
                </div>

                <div className="nav-item">
                    <span>⚑</span>
                    API Testing
                </div>

                <div className="sidebar-bottom">
                    <div className="user-avatar">D</div>
                    <div>
                        <strong>Developer</strong>
                        <small>Testing workspace</small>
                    </div>
                </div>
            </aside>

            <main className="main">
                <header className="topbar">
                    <div>
                        <p className="breadcrumb">Workspace / Overview</p>
                        <h1>Backend Overview</h1>
                        <p className="subtitle">
                            Monitor and test your Feature Flag API.
                        </p>
                    </div>

                    <button
                        className="refresh-button"
                        onClick={() => setTestAgain((count) => count + 1)}
                        disabled={isChecking}
                    >
                        <span className={isChecking ? "spin" : ""}>↻</span>
                        {isChecking ? "Checking..." : "Test connection"}
                    </button>
                </header>

                <section className="welcome-card">
                    <div>
                        <p className="welcome-tag">YOUR FEATURE FLAG SERVICE</p>
                        <h2>Welcome to Flagify</h2>
                        <p>
                            Check your backend status and make sure your API is ready.
                        </p>
                    </div>

                    <div className="welcome-icon">⚡</div>
                </section>

                <section className="stats">
                    <div className="stat-card">
                        <p>Backend Status</p>
                        <div className="stat-value">
              <span
                  className={`status-dot ${
                      isConnected ? "green" : isChecking ? "yellow" : "red"
                  }`}
              />
                            {status}
                        </div>
                        <small>Health endpoint</small>
                    </div>

                    <div className="stat-card">
                        <p>Environment</p>
                        <div className="stat-value">Development</div>
                        <small>Current testing environment</small>
                    </div>

                    <div className="stat-card">
                        <p>Last Checked</p>
                        <div className="stat-value">{lastChecked || "—"}</div>
                        <small>Most recent health check</small>
                    </div>
                </section>

                <section className="content-card">
                    <div className="card-header">
                        <div>
                            <h3>API Health Check</h3>
                            <p>Check whether your backend is responding.</p>
                        </div>

                        <span
                            className={`status-badge ${
                                isConnected
                                    ? "connected"
                                    : isChecking
                                        ? "checking"
                                        : "disconnected"
                            }`}
                        >
              <span className="badge-dot" />
                            {status}
            </span>
                    </div>

                    <div className="endpoint">
                        <span className="method">GET</span>
                        <code>{health_endpoint}</code>
                    </div>

                    <div className="response-header">
                        <span>Response</span>
                        <span className="json-label">JSON</span>
                    </div>

                    <pre className="response-box">
            {response || "Waiting for response..."}
          </pre>
                </section>

                <footer>
                    <span>Flagify</span>
                    <span>Feature Flag Testing Dashboard</span>
                </footer>
            </main>
        </div>
    );
}

export default App;
