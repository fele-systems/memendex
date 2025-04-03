import { Component, EventEmitter, input, Output } from "@angular/core";
import { MemePreviewComponent } from "./meme-preview/meme-preview.component";
import { HttpClient } from "@angular/common/http";
import { Meme } from "../../models/Meme";

@Component({
  selector: "app-meme-gallery",
  imports: [MemePreviewComponent],
  templateUrl: "./meme-gallery.component.html",
  styleUrl: "./meme-gallery.component.css",
})
export class MemeGalleryComponent {
  memes = input.required<Meme[]>();

  @Output() select = new EventEmitter<Meme>();

  onSelectMeme(meme: Meme) {
    this.select.emit(meme);
  }
}
