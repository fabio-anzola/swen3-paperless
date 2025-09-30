"use client";

import { postRegister } from "@/lib/api";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";

export default function RegisterPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const router = useRouter();

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErr(null);

    if (password !== confirmPassword) {
      setErr("Passwords do not match");
      return;
    }

    if (password.length < 6) {
      setErr("Password must be at least 6 characters long");
      return;
    }

    setLoading(true);
    try {
      await postRegister(username, password);
      setSuccess(true);

      router.push("/login");
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <main className="mx-auto mt-24 max-w-md rounded-2xl bg-white p-6 shadow">
        <div className="text-center">
          <h1 className="mb-4 text-2xl font-semibold text-green-600">
            Registration Successful!
          </h1>
          <p className="mb-4 text-gray-600">
            Your account has been created successfully.
          </p>
          <p className="text-sm text-gray-500">Redirecting to login page...</p>
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto mt-24 max-w-md rounded-2xl bg-white p-6 shadow">
      <h1 className="mb-4 text-2xl font-semibold">Create Account</h1>
      <form onSubmit={onSubmit} className="space-y-3">
        <input
          className="w-full rounded border px-3 py-2"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          required
        />
        <input
          className="w-full rounded border px-3 py-2"
          placeholder="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
          required
        />
        <input
          className="w-full rounded border px-3 py-2"
          placeholder="Confirm Password"
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          autoComplete="new-password"
          required
        />
        {err && <p className="text-sm text-red-600">{err}</p>}
        <button
          className="w-full rounded-xl bg-black px-4 py-2 text-white disabled:opacity-50"
          disabled={loading || !username || !password || !confirmPassword}
        >
          {loading ? "Creating Account…" : "Create Account"}
        </button>
      </form>
      <div className="mt-4 text-center">
        <p className="text-sm text-gray-600">
          Already have an account?{" "}
          <Link href="/login" className="text-blue-600 hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </main>
  );
}
