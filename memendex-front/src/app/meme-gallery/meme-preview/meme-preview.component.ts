import {
  Component,
  computed,
  EventEmitter,
  Input,
  OnInit,
  Output,
  signal,
} from "@angular/core";
import { Meme } from "../../../models/Meme";
import { SUPPORTED_THUMBNAILS } from "../../../logic/MimeTypes";
import { MemendexBackendService } from "../../memendex-backend.service";

@Component({
  selector: "app-meme-preview",
  imports: [],
  templateUrl: "./meme-preview.component.html",
  styleUrl: "./meme-preview.component.css",
})
export class MemePreviewComponent implements OnInit {
  @Input({ required: true }) meme!: Meme;
  @Output() select = new EventEmitter<Meme>();

  constructor(private memendexBackend: MemendexBackendService) {}

  onSelectMeme(): void {
    if (this.meme) this.select.emit(this.meme);
  }

  ngOnInit(): void {
    this.memendexBackend.observableThumbnailableExtensions.subscribe(
      (extensions) => {
        this.supportedExtensions.set(extensions);
      },
    );
  }

  supportedExtensions = signal<string[]>([]);
  hasThumbnail = computed(() => {
    const extensions = this.supportedExtensions();
    const meme = this.meme;
    return meme !== undefined && extensions.indexOf(meme.extension) >= 0;
  });
}
