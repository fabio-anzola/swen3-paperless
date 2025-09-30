"use client";

import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import DocumentList from "../components/DocumentList";
import LoadingState from "../components/LoadingState";
import { useDocuments } from "../components/useDocuments";
import { downloadDocument } from "../components/utils";

export default function DocumentsPage() {
  const { token } = useAuth();
  const { documents, error, loading } = useDocuments(token);
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
      <h1 className="text-2xl font-semibold mb-4">Documents</h1>

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
