"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "../auth/AuthContext";

export default function Header() {
  const { token, tokenData, logout } = useAuth();
  const router = useRouter();

  const handleLogout = () => {
    logout();
    router.push("/login");
  };

  if (!token || !tokenData) {
    return null;
  }

  return (
    <header className="bg-white shadow-sm border-b">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center py-4">
          <div className="flex items-center space-x-8">
            <Link href="/dashboard">
              <h1 className="text-xl font-semibold text-gray-900 hover:text-blue-600 transition-colors cursor-pointer">
                Paperless System
              </h1>
            </Link>
            
            <nav className="flex space-x-6">
              <Link 
                href="/dashboard" 
                className="text-sm font-medium text-gray-700 hover:text-blue-600 transition-colors"
              >
                Dashboard
              </Link>
              <Link 
                href="/documents" 
                className="text-sm font-medium text-gray-700 hover:text-blue-600 transition-colors"
              >
                My Documents
              </Link>
              <Link 
                href="/upload" 
                className="text-sm font-medium text-gray-700 hover:text-blue-600 transition-colors"
              >
                Upload
              </Link>
            </nav>
          </div>

          <div className="flex items-center space-x-4">
            <span className="text-sm text-gray-700">
              Welcome, <span className="font-medium">{tokenData.username}</span>
            </span>
            <button
              onClick={handleLogout}
              className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
