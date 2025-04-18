import { Component, EventEmitter, Input, Output } from "@angular/core";
import { Meme } from "../../../models/Meme";
import { SUPPORTED_THUMBNAILS } from "../../../logic/MimeTypes";

@Component({
  selector: "app-meme-preview",
  imports: [],
  templateUrl: "./meme-preview.component.html",
  styleUrl: "./meme-preview.component.css",
})
export class MemePreviewComponent {
  @Input({ required: true }) meme!: Meme;
  @Output() select = new EventEmitter<Meme>();

  onSelectMeme(): void {
    if (this.meme) this.select.emit(this.meme);
  }

  hasThumbnail(): boolean {
    return SUPPORTED_THUMBNAILS.indexOf(this.meme.extension) >= 0;
  }
}
