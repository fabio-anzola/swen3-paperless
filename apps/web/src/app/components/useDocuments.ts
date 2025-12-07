import { api } from "@/config";
import { useEffect, useState } from "react";

export type DocumentDto = {
  id: number;
  name: string;
  s3Key?: string | null;
  ownerId: number;
  summary?: string | null;
  score?: number | null;
};

type Page<T> = { content: T[] };

export function useDocuments(token: string | null, query: string) {
  const [documents, setDocuments] = useState<DocumentDto[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }

    const fetchDocuments = async () => {
      setLoading(true);
      setError(null);

      try {
        const trimmedQuery = query.trim();
        const path =
          trimmedQuery.length > 0
            ? `/document/search?q=${encodeURIComponent(trimmedQuery)}`
            : "/document";

        const res = await fetch(api(path), {
          headers: { Authorization: `Bearer ${token}` },
          cache: "no-store",
        });

        if (!res.ok) {
          throw new Error(
            (await safeText(res)) || `${res.status} ${res.statusText}`
          );
        }

        const page: Page<DocumentDto> = await res.json();
        setDocuments(page.content);
      } catch (e: unknown) {
        setError(e instanceof Error ? e.message : "Failed to load documents");
      } finally {
        setLoading(false);
      }
    };

    fetchDocuments();
  }, [token, query]);

  return { documents, error, loading };
}

async function safeText(res: Response) {
  try {
    return await res.text();
  } catch {
    return null;
  }
}
