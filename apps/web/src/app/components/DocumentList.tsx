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
}

export default function DocumentList({
  documents,
  downloadingId,
  onDownload,
}: DocumentListProps) {
  if (documents.length === 0) {
    return <p className="text-gray-500">No documents found.</p>;
  }

  return (
    <ul className="space-y-2">
      {documents.map((document) => (
        <DocumentItem
          key={document.id}
          document={document}
          isDownloading={downloadingId === document.id}
          onDownload={onDownload}
        />
      ))}
    </ul>
  );
}
