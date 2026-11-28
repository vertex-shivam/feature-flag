# Switchboard frontend

React + Vite frontend for the Feature Flag Spring Boot API.

## Local development

1. Install Node.js 20 or later.
2. Run `npm install`.
3. Copy `.env.example` to `.env` and set `VITE_API_BASE_URL` to the backend URL (default: `http://localhost:8080`).
4. Run `npm run dev`.

## Deploy to Vercel or Netlify

- Build command: `npm run build`
- Output directory: `dist`
- Set `VITE_API_BASE_URL` to the deployed Spring Boot API URL in the host's environment settings.

Both providers have SPA rewrites configured. The backend must allow requests from the deployed frontend origin through CORS.
