import DocumentItem from "./DocumentItem";

type DocumentDto = {
  id: number;
  name: string;
  s3Key?: string | null;
  ownerId: number;
};

interface DocumentListProps {
  documents: DocumentDto[];
  downloadingId: number | null;
  onDownload: (id: number) => void;
  token: string;
}

export default function DocumentList({
  documents,
  downloadingId,
  onDownload,
  token,
}: DocumentListProps) {
  if (documents.length === 0) {
    return (
      <p className="text-gray-300 text-center py-8">No documents found.</p>
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
