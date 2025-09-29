import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const backend =
      process.env.NODE_ENV === "production"
        ? process.env.BACKEND_URL || "http://app:8080"
        : "http://localhost:8080";

    console.log(`Using backend URL: ${backend}`);

    return [{ source: "/api/:path*", destination: `${backend}/api/:path*` }];
  },
};

export default nextConfig;
