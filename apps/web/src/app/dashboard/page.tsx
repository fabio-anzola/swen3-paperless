"use client";

import Link from "next/link";
import { useAuth } from "../auth/AuthContext";

export default function DashboardPage() {
  const { token } = useAuth();

  if (!token) {
    return (
      <main className="mx-auto mt-24 max-w-md rounded-2xl bg-white p-6 shadow">
        <p>
          Please{" "}
          <a className="underline" href="/login">
            login
          </a>{" "}
          first.
        </p>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-4xl p-6">
      <h1 className="text-3xl font-bold text-white mb-8">Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* My Documents Box */}
        <Link href="/documents" className="group">
          <div className="bg-white p-8 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-200 border border-gray-200 group-hover:border-blue-300">
            <div className="flex flex-col items-center text-center space-y-4">
              <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center group-hover:bg-blue-200 transition-colors duration-200">
                <svg
                  className="w-8 h-8 text-blue-600"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
              </div>
              <h2 className="text-xl font-semibold text-gray-800 group-hover:text-blue-600 transition-colors duration-200">
                My Documents
              </h2>
              <p className="text-gray-600">
                View and manage your uploaded documents
              </p>
            </div>
          </div>
        </Link>

        {/* Upload Documents Box */}
        <Link href="/upload" className="group">
          <div className="bg-white p-8 rounded-lg shadow-md hover:shadow-lg transition-shadow duration-200 border border-gray-200 group-hover:border-green-300">
            <div className="flex flex-col items-center text-center space-y-4">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center group-hover:bg-green-200 transition-colors duration-200">
                <svg
                  className="w-8 h-8 text-green-600"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                  />
                </svg>
              </div>
              <h2 className="text-xl font-semibold text-gray-800 group-hover:text-green-600 transition-colors duration-200">
                Upload Documents
              </h2>
              <p className="text-gray-600">
                Upload new documents to your collection
              </p>
            </div>
          </div>
        </Link>
      </div>
    </main>
  );
}
