"use client";

import { getImportRecords, ImportRecordDto } from "@/lib/api";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import LoadingState from "../components/LoadingState";

export default function BatchPage() {
  const { token } = useAuth();
  const [records, setRecords] = useState<ImportRecordDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  const loadRecords = useCallback(async () => {
    if (!token) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await getImportRecords(token);
      setRecords(data);
    } catch (e: unknown) {
      setError(
        e instanceof Error ? e.message : "Failed to load batch imports"
      );
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void loadRecords();
  }, [loadRecords]);

  const filteredRecords = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return records;
    return records.filter(
      (record) =>
        record.description.toLowerCase().includes(term) ||
        record.content.toLowerCase().includes(term)
    );
  }, [records, query]);

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
    <main className="mx-auto max-w-5xl p-6 space-y-5">
      <div className="space-y-2">
        <h1 className="text-3xl font-bold text-white">Batch imports</h1>
        <p className="text-sm text-gray-700 max-w-3xl">
          The batch service watches the shared import folder for XML files and posts
          their content to the API. Use this page to see what has been processed.
        </p>
      </div>

      <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex-1">
          <label className="text-xs font-semibold text-gray-700">
            Filter imports
          </label>
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search description or content"
            className="mt-1 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-gray-900 shadow-sm focus:border-blue-500 focus:outline-none"
          />
        </div>
        <div className="flex flex-col items-start gap-2 sm:items-end">
          <span className="text-sm text-gray-600">
            Showing {filteredRecords.length} of {records.length}
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => void loadRecords()}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 transition-colors"
            >
              Refresh
            </button>
            <button
              onClick={() => setQuery("")}
              className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Clear
            </button>
          </div>
        </div>
      </div>

      <LoadingState loading={loading} error={error}>
        {filteredRecords.length === 0 ? (
          <div className="rounded-lg border border-dashed border-gray-300 bg-white p-8 text-center text-gray-600">
            No imports found yet. Drop XML files into the import folder and they
            will show up here after the next run.
          </div>
        ) : (
          <div className="space-y-3">
            {filteredRecords.map((record) => (
              <article
                key={record.id}
                className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm"
              >
                <div className="flex flex-col gap-1 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="text-xs text-gray-500">
                      Document date: {record.date}
                    </p>
                    <h3 className="text-lg font-semibold text-gray-900">
                      {record.description}
                    </h3>
                  </div>
                  <span className="text-xs text-gray-500">
                    Received {formatDateTime(record.createdAt)}
                  </span>
                </div>
                <div className="mt-2 rounded-md bg-gray-50 p-3 text-sm text-gray-800 whitespace-pre-line">
                  {record.content}
                </div>
              </article>
            ))}
          </div>
        )}
      </LoadingState>
    </main>
  );
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}
