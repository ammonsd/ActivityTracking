import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
    base: "/dashboard/", // Base path for Spring Boot deployment
    server: {
        port: 4201, // Different from Angular (4200)
        proxy: {
            "/api": {
                target: process.env.VITE_BACKEND_URL || "http://localhost:8080",
                changeOrigin: true,
            },
            // Proxy login/logout endpoints for development
            "/login": {
                target: process.env.VITE_BACKEND_URL || "http://localhost:8080",
                changeOrigin: true,
            },
            "/logout": {
                target: process.env.VITE_BACKEND_URL || "http://localhost:8080",
                changeOrigin: true,
            },
        },
    },
});
