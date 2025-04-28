import { Component, EventEmitter, input, Output } from "@angular/core";
import { MemePreviewComponent } from "./meme-preview/meme-preview.component";
import { HttpClient } from "@angular/common/http";
import { Meme } from "../../models/Meme";
import { PaginatedResponse } from "../../models/PaginatedResponse";

@Component({
  selector: "app-meme-gallery",
  imports: [MemePreviewComponent],
  templateUrl: "./meme-gallery.component.html",
  styleUrl: "./meme-gallery.component.css",
})
export class MemeGalleryComponent {
  data = input.required<PaginatedResponse<Meme>>();

  get memes() {
    return this.data().data;
  }

  get totalCount() {
    return this.data().totalCount;
  }

  get pageSize() {
    return this.data().pageSize;
  }

  get currentPage() {
    return this.data().page;
  }

  get hasNext() {
    return this.data().hasNext;
  }

  @Output() select = new EventEmitter<Meme>();
  @Output() requestPage = new EventEmitter<number>();

  onSelectMeme(meme: Meme) {
    this.select.emit(meme);
  }

  onRequestPage(pageNumber: number) {
    this.requestPage.emit(pageNumber);
  }
}
