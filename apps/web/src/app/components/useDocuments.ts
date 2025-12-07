import { api } from "@/config";
import { useCallback, useEffect, useState } from "react";

export type DocumentDto = {
  id: number;
  name: string;
  s3Key?: string | null;
  ownerId: number;
};

type Page<T> = { content: T[] };

export function useDocuments(token: string | null) {
  const [documents, setDocuments] = useState<DocumentDto[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [lastQuery, setLastQuery] = useState<string>("");

  const fetchDocuments = useCallback(
    async (query?: string) => {
      if (!token) {
        setDocuments([]);
        setLastQuery("");
        setLoading(false);
        return;
      }

      const trimmedQuery = query?.trim() ?? "";
      const params = new URLSearchParams();
      if (trimmedQuery) {
        params.set("query", trimmedQuery);
        params.set("q", trimmedQuery);
      }

      setLoading(true);
      setError(null);

      try {
        const res = await fetch(
          trimmedQuery
            ? api(`/document/search?${params.toString()}`)
            : api("/document"),
          {
            headers: { Authorization: `Bearer ${token}` },
            cache: "no-store",
          }
        );

        if (!res.ok) {
          throw new Error(
            (await safeText(res)) || `${res.status} ${res.statusText}`
          );
        }

        const data = await res.json();
        const content = Array.isArray(data)
          ? data
          : (data as Page<DocumentDto>)?.content ?? [];
        setDocuments(content);
        setLastQuery(trimmedQuery);
      } catch (e: unknown) {
        setError(e instanceof Error ? e.message : "Failed to load documents");
      } finally {
        setLoading(false);
      }
    },
    [token]
  );

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  const searchDocuments = useCallback(
    (query: string) => fetchDocuments(query),
    [fetchDocuments]
  );

  return { documents, error, loading, searchDocuments, lastQuery };
}

async function safeText(res: Response) {
  try {
    return await res.text();
  } catch {
    return null;
  }
}
