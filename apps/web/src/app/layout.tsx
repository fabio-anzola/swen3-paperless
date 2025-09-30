import { AuthProvider } from "./auth/AuthContext";
import Header from "./components/Header";
import "./globals.css";

export const metadata = { title: "App", description: "Next + Spring" };

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-gray-50">
        <AuthProvider>
          <Header />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
