"use client";

import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";

type DocumentDto = { id: number; name: string; s3Key?: string | null; ownerId: number };
type Page<T> = { content: T[] };

export default function DocumentsPage() {
  const { token, logout } = useAuth();
  const [docs, setDocs] = useState<DocumentDto[]>([]);
  const [err, setErr] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) { setLoading(false); return; }
    (async () => {
      setLoading(true);
      setErr(null);
      try {
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE}/document`, {
          headers: { Authorization: `Bearer ${token}` },
          cache: "no-store",
        });
        if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
        const page: Page<DocumentDto> = await res.json();
        setDocs(page.content);
      } catch (e: any) {
        setErr(e.message ?? "Failed to load");
      } finally {
        setLoading(false);
      }
    })();
  }, [token]);

  if (!token) {
    return (
      <main className="mx-auto mt-24 max-w-md rounded-2xl bg-white p-6 shadow">
        <p>Please <a className="underline" href="/login">login</a> first.</p>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-3xl p-6 space-y-4">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Documents</h1>
        <button onClick={logout} className="rounded-xl border px-3 py-2">Logout</button>
      </header>

      {loading ? <p>Loading…</p> : err ? <p className="text-red-600">{err}</p> : (
        <ul className="space-y-2">
          {docs.map((d) => (
            <li key={d.id} className="flex items-center justify-between rounded-xl bg-white p-3 shadow">
              <span>{d.name}</span>
              <a
                className="rounded-lg border px-3 py-1"
                href={`${process.env.NEXT_PUBLIC_API_BASE}/document/${d.id}/content`}
                target="_blank"
                rel="noreferrer"
                onClick={(e) => {
                  // plain <a> won't include Authorization header
                  e.preventDefault();
                  alert("For secured downloads, we can add a small proxy route later.");
                }}
              >
                Download
              </a>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}