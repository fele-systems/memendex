export type MemesType = "file" | "link" | "note";

export interface Meme {
  id: number;
  type: MemesType;
  fileName: string;
  description: string;
  extension: string;
  tags: string[];
}
