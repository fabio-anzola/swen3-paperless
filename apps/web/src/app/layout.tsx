import "./globals.css";
import { AuthProvider } from "./auth/AuthContext";

export const metadata = { title: "App", description: "Next + Spring" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-gray-50">
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}