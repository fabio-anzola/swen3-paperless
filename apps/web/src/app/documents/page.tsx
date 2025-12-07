"use client";

import { FormEvent, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import DocumentList from "../components/DocumentList";
import LoadingState from "../components/LoadingState";
import { useDocuments } from "../components/useDocuments";
import { downloadDocument } from "../components/utils";

export default function DocumentsPage() {
  const { token } = useAuth();
  const { documents, error, loading, searchDocuments, lastQuery } =
    useDocuments(token);
  const [searchTerm, setSearchTerm] = useState<string>("");
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

  const handleSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await searchDocuments(searchTerm);
  };

  const handleClearSearch = async () => {
    setSearchTerm("");
    await searchDocuments("");
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

      <form
        onSubmit={handleSearch}
        className="bg-white/10 backdrop-blur p-4 rounded-xl border border-white/10 flex flex-col gap-3 sm:flex-row sm:items-center"
      >
        <div className="flex-1 w-full">
          <label className="sr-only" htmlFor="document-search">
            Search documents
          </label>
          <input
            id="document-search"
            type="search"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search your documents..."
            className="w-full rounded-lg border border-gray-300 bg-white px-4 py-2 text-gray-900 shadow-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-200 transition-colors"
          />
        </div>
        <div className="flex gap-2">
          <button
            type="submit"
            disabled={loading}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-300 disabled:opacity-60 disabled:cursor-not-allowed"
          >
            Search
          </button>
          {lastQuery && (
            <button
              type="button"
              onClick={handleClearSearch}
              className="rounded-lg bg-gray-200 px-4 py-2 text-sm font-semibold text-gray-800 shadow hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-300"
            >
              Clear
            </button>
          )}
        </div>
      </form>

      {lastQuery ? (
        <p className="text-sm text-gray-200">
          Showing search results for{" "}
          <span className="text-white">&quot;{lastQuery}&quot;</span>
        </p>
      ) : (
        <p className="text-sm text-gray-200">Showing your latest documents.</p>
      )}

      <LoadingState loading={loading} error={error}>
        <DocumentList
          documents={documents}
          downloadingId={downloadingId}
          onDownload={handleDownload}
        />
      </LoadingState>
    </main>
  );
}
