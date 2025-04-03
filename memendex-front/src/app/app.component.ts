import { Component, signal } from "@angular/core";
import { RouterOutlet } from "@angular/router";
import { MemeUploadComponent } from "./meme-upload/meme-upload.component";
import { MemeGalleryComponent } from "./meme-gallery/meme-gallery.component";
import { MemeDetailsComponent } from "./meme-details/meme-details.component";
import { Meme } from "../models/Meme";
import { HttpClient } from "@angular/common/http";
import { MemeSearchComponent } from "./meme-search/meme-search.component";
import { DescriptionTextAreaComponent } from "./controls/description-text-area/description-text-area.component";

@Component({
  selector: "app-root",
  imports: [
    RouterOutlet,
    MemeUploadComponent,
    MemeGalleryComponent,
    MemeDetailsComponent,
    MemeSearchComponent,
    DescriptionTextAreaComponent,
  ],
  templateUrl: "./app.component.html",
  styleUrl: "./app.component.css",
})
export class AppComponent {
  title = "memendex-front";

  selectedMeme: Meme | undefined;
  memes = signal<Meme[]>([]);

  constructor(private http: HttpClient) {
    this.http.get("/api/memes/list", { observe: "response" }).subscribe(
      (response) => {
        this.memes.set(response.body as Meme[]);
      },
      (error) => {
        console.error("Request failed:", error);
        alert(error);
      },
    );
  }

  onSelectMeme(meme: Meme) {
    this.selectedMeme = meme;
  }

  onSearchCompleted(memes: Meme[]) {
    this.memes.set(memes);
  }

  onMemeUploaded(meme: Meme) {
    this.memes.update((values) => [...values, meme]);
  }

  onSearchReseted() {
    this.http.get("/api/memes/list", { observe: "response" }).subscribe(
      (response) => {
        this.memes.set(response.body as Meme[]);
      },
      (error) => {
        console.error("Request failed:", error);
        alert(error);
      },
    );
  }
}
