"use client";

import { api } from "@/config";
import { useEffect, useState } from "react";

class DownloadError extends Error {
  status?: number;
  constructor(message: string, status?: number) {
    super(message);
    this.status = status;
  }
}

export default function SharePage({
  params,
}: {
  params: { token: string };
}) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [needsPassword, setNeedsPassword] = useState(false);
  const [downloaded, setDownloaded] = useState(false);

  // First try without a password; if backend says password is required, show the form
  useEffect(() => {
    const tryWithoutPassword = async () => {
      setError(null);
      try {
        await performDownload(params.token, "");
        setDownloaded(true);
        setNeedsPassword(false);
      } catch (err: unknown) {
        const e = err as DownloadError;
        const message = e instanceof Error ? e.message : "Download failed";
        if (
          e instanceof DownloadError &&
          e.status === 403 &&
          message.toLowerCase().includes("password")
        ) {
          setNeedsPassword(true);
          setError("This link is protected. Please enter the password.");
        } else {
          setNeedsPassword(false);
          setError(message);
        }
      } finally {
        setLoading(false);
      }
    };

    void tryWithoutPassword();
  }, [params.token]);

  const handleDownload = async (e?: React.FormEvent) => {
    e?.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await performDownload(params.token, password);
      setDownloaded(true);
      setNeedsPassword(false);
    } catch (err: unknown) {
      const e = err as DownloadError;
      setError(e instanceof Error ? e.message : "Download failed");
    } finally {
      setLoading(false);
    }
  };

  const showPasswordForm = needsPassword;

  return (
    <main className="mx-auto mt-24 max-w-xl rounded-2xl bg-white p-6 shadow space-y-4">
      <div className="space-y-1">
        <p className="text-xs uppercase tracking-wide text-gray-500">
          Shared document
        </p>
        <h1 className="text-2xl font-semibold text-gray-900">
          {showPasswordForm ? "Enter password to download" : "Download document"}
        </h1>
        <p className="text-sm text-gray-600">
          Token: <span className="font-mono text-gray-800">{params.token}</span>
        </p>
      </div>

      {loading && (
        <p className="text-sm text-gray-600">Preparing your download…</p>
      )}

      {error && !loading && <p className="text-sm text-red-600">{error}</p>}

      {showPasswordForm && !loading && (
        <form onSubmit={handleDownload} className="space-y-3">
          <div className="space-y-1">
            <label className="text-sm font-medium text-gray-800">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900"
              placeholder="Enter password"
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
          >
            {loading ? "Downloading…" : "Download document"}
          </button>
        </form>
      )}

      {!showPasswordForm && !loading && (
        <div className="space-y-3">
          {downloaded && (
            <p className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-md px-3 py-2">
              Download started. Need it again? Use the button below.
            </p>
          )}
          <button
            type="button"
            onClick={() => void handleDownload()}
            className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
          >
            Download document
          </button>
        </div>
      )}
    </main>
  );
}

async function performDownload(token: string, password: string) {
  const query = password ? `?password=${encodeURIComponent(password)}` : "";
  const res = await fetch(api(`/public/share/${token}${query}`));

  if (!res.ok) {
    const body = await safeText(res);
    throw new DownloadError(body || `${res.status} ${res.statusText}`, res.status);
  }

  const cd = res.headers.get("content-disposition") || "";
  const match = /filename\*?=([^;]+)/i.exec(cd);
  const raw = match
    ? match[1]
        .trim()
        .replace(/^UTF-8''/, "")
        .replace(/"/g, "")
    : `shared-document-${token}`;
  const filename = decodeURIComponent(raw);

  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

async function safeText(res: Response) {
  try {
    const txt = await res.text();
    try {
      const json = JSON.parse(txt);
      return json?.detail || json?.message || txt;
    } catch {
      return txt;
    }
  } catch {
    return null;
  }
}
