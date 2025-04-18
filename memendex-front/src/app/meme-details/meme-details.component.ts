import {
  Component,
  input,
  Input,
  OnChanges,
  OnInit,
  output,
  SimpleChanges,
} from "@angular/core";
import { Meme } from "../../models/Meme";
import { DescriptionTextAreaComponent } from "../controls/description-text-area/description-text-area.component";
import { FormControl, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { HttpClient } from "@angular/common/http";
import { TagsInputComponent } from "../controls/tags-input/tags-input.component";
import { SUPPORTED_THUMBNAILS } from "../../logic/MimeTypes";

@Component({
  selector: "app-meme-details",
  imports: [
    FormsModule,
    DescriptionTextAreaComponent,
    ReactiveFormsModule,
    TagsInputComponent,
  ],
  templateUrl: "./meme-details.component.html",
  styleUrl: "./meme-details.component.css",
})
export class MemeDetailsComponent implements OnChanges, OnInit {
  // @Input({ required: false }) meme: Meme | undefined;
  meme = input<Meme>();
  description = new FormControl("");
  tags = new FormControl([] as string[]);
  memeUpdated = output<Meme>();

  constructor(private http: HttpClient) {}

  ngOnInit(): void {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["meme"] && this.meme) {
      // Update the FormControl value when `object` changes
      this.description.setValue(this.meme()?.description || "");
      this.tags.setValue(this.meme()?.tags || []);
    }
  }

  compareArrays<T>(a: T[], b: T[]) {
    return (
      a.length === b.length && a.every((element, index) => element === b[index])
    );
  }

  hasThumbnail(): boolean {
    return (
      this.meme() !== undefined &&
      SUPPORTED_THUMBNAILS.indexOf(this.meme()!.extension) >= 0
    );
  }

  submitUpdate() {
    var payload = new Map<string, unknown>();
    payload.set("id", this.meme()?.id as number);

    if (this.meme()?.description !== this.description.value)
      payload.set("description", this.description.value || "");
    if (!this.compareArrays(this.meme()?.tags || [], this.tags.value || []))
      payload.set("tags", this.tags.value);

    this.http
      .patch("/api/memes/edit", Object.fromEntries(payload.entries()), {
        observe: "response",
      })
      .subscribe((response) => {
        if (!response.ok) {
          alert(JSON.stringify(response.body));
        }
        this.memeUpdated.emit(response.body as Meme);
      });
  }
}
