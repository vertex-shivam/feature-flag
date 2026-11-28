import { useEffect, useState } from "react";

function App() {
    const [status, setStatus] = useState("Checking...");

    useEffect(() => {
        fetch(`${import.meta.env.BASE_URL}/api/health`)
            .then((response) => {
                if (response.ok) {
                    setStatus("Backend Connected");
                } else {
                    setStatus("Backend Error");
                }
            })
            .catch(() => {
                setStatus("Backend Not Connected");
            });
    }, []);

    return (
        <div>
            <h1>Feature Flag</h1>
            <h2>Backend Status: {status}</h2>
        </div>
    );
}

export default App;