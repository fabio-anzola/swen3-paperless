import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    // In Docker, use the service name 'app' for container-to-container communication
    // For local development, use localhost:4000 (the mapped port)
    const backend = process.env.NODE_ENV === 'production' 
      ? process.env.BACKEND_URL || "http://app:8080"
      : "http://localhost:4000";
    
    console.log(`Using backend URL: ${backend}`);
    
    return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
  },
};

export default nextConfig;
