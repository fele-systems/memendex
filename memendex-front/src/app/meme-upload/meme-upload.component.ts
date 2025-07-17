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

/**
 * This component handles the variations in fields
 * for each different meme type.
 */
@Component({
  selector: "app-meme-upload",
  imports: [FormsModule, ReactiveFormsModule, DescriptionTextAreaComponent],
  templateUrl: "./meme-upload.component.html",
  styleUrl: "./meme-upload.component.css",
})
export class MemeUploadComponent {
  /** Field exclusive for Bookmark Memes */
  link = new FormControl<string>("");
  /** Field exclusive for Note Memes */
  title = new FormControl<string>("");
  /** Fields exclusive for Image or File Memes */
  file: File | undefined;
  fileFormControl = new FormControl();
  /** Field commom for all types of Memes */
  description = new FormControl<string>("");
  /** Error reporter variable */
  error?: string;

  /** Signal for when a Meme submit is completed sucessfuly */
  @Output() memeUploaded = new EventEmitter<Meme>();

  constructor(private http: HttpClient) {}

  /**
   * Constructs a FormData containing the following data depending on meme file:
   * - when file: file, description, type
   * - when note: title, description, type
   * - when link: link, description, type
   * and submits it to `/api/memes/upload`.
   * The meme type is guesses based on which fields are filled (title, link, file).
   */
  submit(): void {
    const formData = new FormData();

    const hasLink = Boolean(this.link.value);
    const hasTitle = Boolean(this.title.value);
    const hasFile = Boolean(this.file);

    // Guess memeType based on which field is filled
    const memeType =
      hasLink && !hasTitle && !hasFile
        ? "link"
        : hasTitle && !hasLink && !hasFile
          ? "note"
          : hasFile && !hasTitle && !hasLink
            ? "file"
            : undefined;

    if (memeType === undefined) {
      throw Error(
        "Invalid meme type: could not guess meme type based on which inputs where filled",
      );
    }

    // Common data for all types
    if (this.description.value)
      formData.append("description", this.description.value);
    formData.append("type", memeType);

    // Validade the fields exclusive for each meme and fill FormData
    if (memeType === "file") {
      if (!this.file) {
        this.error = "Please fill the file input";
        return;
      }
      formData.append("file", this.file);
    } else if (memeType === "note") {
      if (!this.title.value) {
        this.error = "Please fill title input";
        return;
      }
      formData.append("title", this.title.value);
    } else if (memeType === "link") {
      if (!this.link.value) {
        this.error = "Please fill the link input";
        return;
      }
      formData.append("link", this.link.value);
    }

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

        this.file = undefined;
        this.link.reset();
        this.title.reset();
        this.description.reset();
        this.fileFormControl.reset();
        this.link.enable();
        this.title.enable();
        this.fileFormControl.enable();
      }
    });
  }

  onFileSelected(event: Event) {
    if (event.target) {
      const element = event.target as HTMLInputElement;
      this.file = element.files?.item(0) || undefined;
      this.autoFillDescription(false);

      this.link.disable();
      this.title.disable();
    }
  }

  onLinkChanged(event: Event) {
    if (this.link.value) {
      this.title.disable();
      this.fileFormControl.disable();
    } else {
      this.title.enable();
      this.fileFormControl.enable();
    }
  }

  onTitleChanged(event: Event) {
    if (this.title.value) {
      this.fileFormControl.disable();
      this.link.disable();
    } else {
      this.fileFormControl.enable();
      this.link.enable();
    }
  }

  autoFillDescription(overwrite: boolean) {
    if (this.file !== undefined) {
      if (overwrite || !this.description.value) {
        this.description.setValue(this.file?.name || "");
      }
    }
  }
}
