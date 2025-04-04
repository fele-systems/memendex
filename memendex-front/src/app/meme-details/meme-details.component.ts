import {
  Component,
  input,
  Input,
  OnChanges,
  SimpleChanges,
} from "@angular/core";
import { Meme } from "../../models/Meme";
import { DescriptionTextAreaComponent } from "../controls/description-text-area/description-text-area.component";
import { FormControl, FormsModule, ReactiveFormsModule } from "@angular/forms";
import { HttpClient } from "@angular/common/http";

@Component({
  selector: "app-meme-details",
  imports: [FormsModule, DescriptionTextAreaComponent, ReactiveFormsModule],
  templateUrl: "./meme-details.component.html",
  styleUrl: "./meme-details.component.css",
})
export class MemeDetailsComponent implements OnChanges {
  // @Input({ required: false }) meme: Meme | undefined;
  meme = input<Meme>();
  description = new FormControl("");

  constructor(private http: HttpClient) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["meme"] && this.meme) {
      // Update the FormControl value when `object` changes
      this.description.setValue(this.meme()?.description || "");
    }
  }

  submitUpdate() {
    console.log(`Original: ${this.meme()?.description}`);
    console.log(`Current: ${this.description.value}`);

    this.http
      .patch(
        "/api/memes/edit",
        { id: this.meme()?.id, description: this.description.value },
        {
          observe: "response",
        },
      )
      .subscribe((response) => {
        if (!response.ok) {
          alert(JSON.stringify(response.body));
        }
      });
  }
}
