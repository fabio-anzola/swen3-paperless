"use client";

import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import DocumentList from "../components/DocumentList";
import LoadingState from "../components/LoadingState";
import { useDocuments } from "../components/useDocuments";
import { downloadDocument } from "../components/utils";

export default function DocumentsPage() {
  const { token } = useAuth();
  const [query, setQuery] = useState("");
  const { documents, error, loading } = useDocuments(token, query);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  const handleDownload = async (id: number) => {
    if (!token) return;

    setDownloadingId(id);
    try {
      await downloadDocument(id, token);
    } catch (e: unknown) {
      console.error(
        "Download failed:",
        e instanceof Error ? e.message : "Download failed"
      );
    } finally {
      setDownloadingId(null);
    }
  };

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
      <h1 className="text-3xl font-bold text-white mb-6">Documents</h1>

      <div className="rounded-xl bg-white/70 backdrop-blur px-4 py-3 shadow flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex-1 flex items-center gap-2">
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by name, summary, or content"
            className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-gray-900 shadow-sm focus:border-indigo-500 focus:outline-none"
          />
        </div>
        {query && (
          <button
            className="text-sm text-indigo-700 hover:underline mt-2 sm:mt-0"
            onClick={() => setQuery("")}
          >
            Clear search
          </button>
        )}
      </div>

      <LoadingState loading={loading} error={error}>
        <DocumentList
          documents={documents}
          downloadingId={downloadingId}
          onDownload={handleDownload}
          query={query}
        />
      </LoadingState>
    </main>
  );
}
