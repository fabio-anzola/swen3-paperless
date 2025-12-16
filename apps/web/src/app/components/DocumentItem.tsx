"use client";

import {
  createDocumentShare,
  DocumentShareDto,
  DocumentShareLogDto,
  getDocumentShareLogs,
  getDocumentShares,
  deactivateDocumentShare,
} from "@/lib/api";
import { useEffect, useMemo, useState } from "react";

interface DocumentItemProps {
  document: {
    id: number;
    name: string;
    s3Key?: string | null;
    ownerId: number;
  };
  isDownloading: boolean;
  onDownload: (id: number) => void;
  token: string;
}

export default function DocumentItem({
  document,
  isDownloading,
  onDownload,
  token,
}: DocumentItemProps) {
  const [shareOpen, setShareOpen] = useState(false);
  const [shares, setShares] = useState<DocumentShareDto[]>([]);
  const [logs, setLogs] = useState<DocumentShareLogDto[]>([]);
  const [loadingShares, setLoadingShares] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [password, setPassword] = useState("");
  const [startsAt, setStartsAt] = useState("");
  const [expiresAt, setExpiresAt] = useState("");

  const shareBaseUrl = useMemo(
    () => (typeof window !== "undefined" ? window.location.origin : ""),
    []
  );

  useEffect(() => {
    if (shareOpen) {
      void loadSharesAndLogs();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shareOpen]);

  const loadSharesAndLogs = async () => {
    setLoadingShares(true);
    setError(null);
    try {
      const [s, l] = await Promise.all([
        getDocumentShares(document.id, token),
        getDocumentShareLogs(document.id, token),
      ]);
      setShares(s);
      setLogs(l);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load shares");
    } finally {
      setLoadingShares(false);
    }
  };

  const handleCreateShare = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreating(true);
    setError(null);
    try {
      await createDocumentShare(document.id, token, {
        password: password || undefined,
        startsAt: startsAt ? new Date(startsAt).toISOString() : undefined,
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined,
      });
      setPassword("");
      setStartsAt("");
      setExpiresAt("");
      await loadSharesAndLogs();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to create share");
    } finally {
      setCreating(false);
    }
  };

  const formatDate = (value?: string | null) => {
    if (!value) return "—";
    const date = new Date(value);
    return date.toLocaleString();
  };

  const shareUrl = (tokenValue: string) =>
    shareBaseUrl ? `${shareBaseUrl}/share/${tokenValue}` : "";

  const handleDeactivateShare = async (shareId: number) => {
    setLoadingShares(true);
    setError(null);
    try {
      await deactivateDocumentShare(document.id, shareId, token);
      await loadSharesAndLogs();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to deactivate share");
    } finally {
      setLoadingShares(false);
    }
  };

  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      setError("Could not copy to clipboard");
    }
  };

  return (
    <li className="rounded-xl bg-white p-4 shadow space-y-3">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-gray-900 font-semibold">{document.name}</p>
        </div>
        <div className="flex gap-2">
          <button
            className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            disabled={isDownloading}
            onClick={() => onDownload(document.id)}
          >
            {isDownloading ? "Downloading…" : "Download"}
          </button>
          <button
            className="rounded-lg border border-blue-200 bg-blue-50 px-3 py-2 text-sm font-medium text-blue-700 hover:bg-blue-100 transition-colors"
            onClick={() => setShareOpen((v) => !v)}
          >
            {shareOpen ? "Hide sharing" : "Share"}
          </button>
        </div>
      </div>

      {shareOpen && (
        <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-gray-900 font-semibold text-sm">
              Share settings
            </h3>
            {loadingShares && (
              <span className="text-xs text-gray-500">Loading…</span>
            )}
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <form onSubmit={handleCreateShare} className="grid gap-3 md:grid-cols-3">
            <div className="space-y-1">
              <label className="text-xs font-medium text-gray-700">
                Password (optional)
              </label>
              <input
                type="text"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-2 py-2 text-sm text-gray-900"
                placeholder="Set a password"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium text-gray-700">
                Starts at (optional)
              </label>
              <input
                type="datetime-local"
                value={startsAt}
                onChange={(e) => setStartsAt(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-2 py-2 text-sm text-gray-900"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium text-gray-700">
                Expires at (optional)
              </label>
              <input
                type="datetime-local"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                className="w-full rounded-md border border-gray-300 px-2 py-2 text-sm text-gray-900"
              />
            </div>
            <div className="md:col-span-3 flex justify-end">
              <button
                type="submit"
                disabled={creating}
                className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {creating ? "Creating…" : "Create share link"}
              </button>
            </div>
          </form>

          <div className="space-y-2">
            <p className="text-xs font-semibold text-gray-700">Active links</p>
            {shares.length === 0 ? (
              <p className="text-sm text-gray-500">
                No share links yet. Create one above.
              </p>
            ) : (
              <div className="space-y-2">
                {shares.map((share) => (
                  <div
                    key={share.id}
                    className="rounded-md border border-gray-200 bg-white p-3 space-y-1"
                  >
                    <div className="flex flex-wrap items-center gap-2 text-sm">
                      <span className="font-medium text-gray-900">
                        Token: {share.token.slice(0, 8)}…
                      </span>
                      {share.passwordProtected && (
                        <span className="rounded-full bg-gray-100 px-2 py-1 text-xs text-gray-700">
                          Password protected
                        </span>
                      )}
                      {!share.active && (
                        <span className="rounded-full bg-yellow-100 px-2 py-1 text-xs text-yellow-800">
                          Inactive
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-600">
                      Starts: {formatDate(share.startsAt)} · Expires:{" "}
                      {formatDate(share.expiresAt)}
                    </p>
                    <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
                      <div className="flex-1">
                        <input
                          readOnly
                          value={shareUrl(share.token)}
                          className="w-full rounded-md border border-gray-300 px-2 py-2 text-sm text-gray-900"
                          onFocus={(e) => e.currentTarget.select()}
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => copyToClipboard(shareUrl(share.token))}
                        className="mt-2 md:mt-0 rounded-md bg-gray-900 px-3 py-2 text-sm font-medium text-white hover:bg-black"
                      >
                        Copy link
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDeactivateShare(share.id)}
                        className="mt-2 md:mt-0 inline-flex items-center justify-center rounded-md border border-red-200 bg-red-50 px-2 py-2 text-xs font-medium text-red-700 hover:bg-red-100 disabled:opacity-60"
                        disabled={!share.active}
                        title={share.active ? "Deactivate link" : "Inactive"}
                      >
                        🗑️
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="space-y-2">
            <p className="text-xs font-semibold text-gray-700">Access log</p>
            {logs.length === 0 ? (
              <p className="text-sm text-gray-500">No access yet.</p>
            ) : (
              <div className="overflow-hidden rounded-md border border-gray-200 bg-white">
                <table className="min-w-full text-sm">
                  <thead className="bg-gray-50 text-left text-gray-600">
                    <tr>
                      <th className="px-3 py-2 font-medium">When</th>
                      <th className="px-3 py-2 font-medium">Result</th>
                      <th className="px-3 py-2 font-medium">IP</th>
                      <th className="px-3 py-2 font-medium">Reason</th>
                    </tr>
                  </thead>
                  <tbody>
                    {logs.map((log) => (
                      <tr key={`${log.shareId}-${log.accessedAt}`}>
                        <td className="px-3 py-2 text-gray-900">
                          {formatDate(log.accessedAt)}
                        </td>
                        <td className="px-3 py-2">
                          <span
                            className={`rounded-full px-2 py-1 text-xs ${
                              log.success
                                ? "bg-green-100 text-green-700"
                                : "bg-red-100 text-red-700"
                            }`}
                          >
                            {log.success ? "Success" : "Denied"}
                          </span>
                        </td>
                        <td className="px-3 py-2 text-gray-700">
                          {log.remoteAddress || "—"}
                        </td>
                        <td className="px-3 py-2 text-gray-700">
                          {log.reason || "—"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </li>
  );
}
