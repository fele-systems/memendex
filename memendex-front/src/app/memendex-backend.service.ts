import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";

@Injectable({
  providedIn: "root",
})
export class MemendexBackendService {
  constructor(http: HttpClient) {
    this.getThumbnailableExtensions();
  }

  private thumbnailableExtensionsSubject = new BehaviorSubject<string[]>([]);
  observableThumbnailableExtensions =
    this.thumbnailableExtensionsSubject.asObservable();

  private async getThumbnailableExtensions(): Promise<void> {
    const response = await fetch("/api/mime/known");

    if (response.ok) {
      const extensions = (await response.json()) as {
        mimeType: string;
        extension: string;
      }[];

      this.thumbnailableExtensionsSubject.next(
        extensions.map((m) => m.extension),
      );
    } else {
      console.error(
        "Could not fetch list of supported thumbnail extensions. Limiting to known image types.",
      );
      this.thumbnailableExtensionsSubject.next(["jpeg", "png", "gif"]);
    }
  }
}
