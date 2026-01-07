import DocumentItem from "./DocumentItem";
import { DocumentDto } from "./useDocuments";

interface DocumentListProps {
  documents: DocumentDto[];
  downloadingId: number | null;
  onDownload: (id: number) => void;
  query?: string;
  token: string;
}

export default function DocumentList({
  documents,
  downloadingId,
  onDownload,
  query,
  token,
}: DocumentListProps) {
  if (documents.length === 0) {
    return (
      <p className="text-gray-300 text-center py-8">
        {query ? "No documents match your search." : "No documents found."}
      </p>
    );
  }

  return (
    <ul className="space-y-3">
      {documents.map((document) => (
        <DocumentItem
          key={document.id}
          document={document}
          isDownloading={downloadingId === document.id}
          onDownload={onDownload}
          token={token}
        />
      ))}
    </ul>
  );
}
