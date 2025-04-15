import { Component, EventEmitter, Output } from "@angular/core";
import { Meme } from "../../models/Meme";
import { HttpClient, HttpEventType } from "@angular/common/http";
import {
  FormControl,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { CommonModule } from "@angular/common";
import { PaginatedResponse } from "../../models/PaginatedResponse";

@Component({
  selector: "app-meme-search",
  imports: [FormsModule, ReactiveFormsModule],
  templateUrl: "./meme-search.component.html",
  styleUrl: "./meme-search.component.css",
})
export class MemeSearchComponent {
  @Output() searchCompleted = new EventEmitter<PaginatedResponse<Meme>>();
  @Output() resetSearch = new EventEmitter();
  constructor(private http: HttpClient) {}

  query = new FormControl<string | undefined>(undefined, [
    Validators.minLength(3),
  ]);

  submit() {
    if (this.query.value) {
      const response = this.http.get("/api/memes/search", {
        params: {
          query: this.query.value,
        },
        observe: "response",
      });

      response.subscribe((event) => {
        this.searchCompleted.emit(event.body as PaginatedResponse<Meme>);
      });
    }
  }

  onQueryChanged(event: KeyboardEvent) {
    if (this.query.value) {
      if (this.query.value.length === 1 && event.key === "Backspace") {
        this.resetSearch.emit();
      }
    }
  }
}
