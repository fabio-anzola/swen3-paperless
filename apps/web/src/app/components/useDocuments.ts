import { api } from "@/config";
import { useEffect, useState } from "react";

type DocumentDto = {
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

  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }

    const fetchDocuments = async () => {
      setLoading(true);
      setError(null);

      try {
        const res = await fetch(api("/document"), {
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
  }, [token]);

  return { documents, error, loading };
}

async function safeText(res: Response) {
  try {
    return await res.text();
  } catch {
    return null;
  }
}
