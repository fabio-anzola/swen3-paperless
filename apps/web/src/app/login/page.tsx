"use client";

import { postLogin } from "@/lib/api";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const { setToken } = useAuth();
  const router = useRouter();

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);
    setLoading(true);
    try {
      const token = await postLogin(username, password);
      setToken(token);
      router.push("/documents");
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="mx-auto mt-24 max-w-md rounded-2xl bg-white p-8 shadow-lg">
      <h1 className="mb-6 text-3xl font-bold text-gray-900">Sign in</h1>
      <form onSubmit={onSubmit} className="space-y-4">
        <input
          className="w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
        />
        <input
          className="w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          placeholder="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
        />
        {err && <p className="text-sm text-red-600 font-medium">{err}</p>}
        <button
          className="w-full rounded-lg bg-black px-4 py-3 text-white font-medium hover:bg-gray-800 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          disabled={loading}
        >
          {loading ? "Signing in…" : "Sign in"}
        </button>
      </form>
      <div className="mt-6 text-center">
        <p className="text-sm text-gray-700">
          Don&apos;t have an account?{" "}
          <Link
            href="/register"
            className="text-blue-600 font-medium hover:underline"
          >
            Create one
          </Link>
        </p>
      </div>
    </main>
  );
}
