import { Component, signal } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { MemeUploadComponent } from "./meme-upload/meme-upload.component";
import { MemeGalleryComponent } from "./meme-gallery/meme-gallery.component";
import { MemeDetailsComponent } from "./meme-details/meme-details.component";
import { Meme } from "../models/Meme";
import { HttpClient } from "@angular/common/http";
import { MemeSearchComponent } from "./meme-search/meme-search.component";
import { DescriptionTextAreaComponent } from "./controls/description-text-area/description-text-area.component";
import { PaginatedResponse } from "../models/PaginatedResponse";

@Component({
  selector: "app-root",
  imports: [
    RouterOutlet,
    MemeUploadComponent,
    MemeGalleryComponent,
    MemeDetailsComponent,
    MemeSearchComponent,
  ],
  templateUrl: "./app.component.html",
  styleUrl: "./app.component.css",
})
export class AppComponent {
  title = "memendex-front";

  selectedMeme: Meme | undefined;
  // memes = signal<Meme[]>([]);
  defaultPageSize = 100;
  // totalMemes = 0;
  memes = signal<PaginatedResponse<Meme>>({
    data: [],
    count: 0,
    totalCount: 0,
    pageSize: 0,
    page: 0,
    hasNext: false,
  });

  constructor(private http: HttpClient) {
    this.fetchMemes(this.defaultPageSize, 1);
  }

  fetchMemes(pageSize: number, pageNum: number) {
    this.http
      .get("/api/memes/list", {
        params: { page: pageNum, size: pageSize },
        observe: "response",
      })
      .subscribe((response) => {
        var page = response.body as PaginatedResponse<Meme>;
        this.memes.set(page);
      });
  }

  memeUpdated(meme: Meme) {
    const i = this.memes().data.findIndex((m) => m.id === meme.id);
    if (i >= 0) {
      Object.assign(this.memes().data[i], meme);
      this.memes.set(this.memes());
    }
  }

  onSelectMeme(meme: Meme) {
    this.selectedMeme = meme;
  }

  onSearchCompleted(memes: PaginatedResponse<Meme>) {
    this.memes.set(memes);
  }

  onMemeUploaded(meme: Meme) {
    this.fetchMemes(this.defaultPageSize, 1);
  }

  onSearchReseted() {
    this.fetchMemes(this.defaultPageSize, 1);
  }
}
