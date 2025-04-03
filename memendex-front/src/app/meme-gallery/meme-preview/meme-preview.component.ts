import { Component, EventEmitter, Input, Output } from "@angular/core";
import { Meme } from "../../../models/Meme";

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
}
