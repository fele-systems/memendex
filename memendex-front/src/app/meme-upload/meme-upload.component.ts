import { HttpClient, HttpEventType, HttpHeaders } from "@angular/common/http";
import {
  Component,
  ElementRef,
  EventEmitter,
  Output,
  ViewChild,
} from "@angular/core";
import {
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
} from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { DescriptionTextAreaComponent } from "../controls/description-text-area/description-text-area.component";
import { Meme } from "../../models/Meme";

@Component({
  selector: "app-meme-upload",
  imports: [FormsModule, ReactiveFormsModule, DescriptionTextAreaComponent],
  templateUrl: "./meme-upload.component.html",
  styleUrl: "./meme-upload.component.css",
})
export class MemeUploadComponent {
  uploadForm = new FormGroup({
    meme: new FormControl(""),
    description: new FormControl<string>(""),
  });
  file: File | undefined;
  @Output() memeUploaded = new EventEmitter<Meme>();

  constructor(private http: HttpClient) {}

  submit() {
    const formData = new FormData();
    const { description } = this.uploadForm.value;

    if (!this.file || !description) {
      alert("Please fill both the meme file and a description to upload");
      return;
    }

    formData.append("description", description || "");
    formData.append("meme", this.file);

    const response = this.http.post("/api/memes/upload", formData, {
      observe: "events",
    });

    response.subscribe((event) => {
      if (event.type === HttpEventType.UploadProgress) {
        console.log(`Uploaded ${event.loaded} of ${event.total}`);
      } else if (event.type === HttpEventType.Response) {
        if (event.status !== 200) {
          alert(`Something went wrong: ${JSON.stringify(event.body)}`);
        } else {
          console.log("Emmiting meme", JSON.stringify(event.body));
          this.memeUploaded.emit(event.body as Meme);
        }

        this.uploadForm.reset();
      }
    });
  }

  onFileSelected(event: Event) {
    if (event.target) {
      const element = event.target as HTMLInputElement;
      this.file = element.files?.item(0) || undefined;
      this.autoFillDescription(false);
    }
  }

  autoFillDescription(overwrite: boolean) {
    if (this.file !== undefined) {
      const description = this.uploadForm.get("description");
      if (description && (overwrite || description.value === "")) {
        description.setValue(this.file?.name || "");
      }
    }
  }
}
