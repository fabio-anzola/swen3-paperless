"use client";

import { useAuth } from "../auth/AuthContext";

export default function UploadPage() {
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
    <main className="mx-auto max-w-3xl p-6 space-y-4">
      <h1 className="text-2xl font-semibold mb-4">Upload Documents</h1>
      
      <div className="bg-white p-8 rounded-lg shadow-md border border-gray-200">
        <p className="text-gray-600 text-center">
          Document upload functionality will be implemented here.
        </p>
      </div>
    </main>
  );
}