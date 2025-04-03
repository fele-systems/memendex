import { Component, Input } from "@angular/core";
import { Meme } from "../../models/Meme";

@Component({
  selector: "app-meme-details",
  imports: [],
  templateUrl: "./meme-details.component.html",
  styleUrl: "./meme-details.component.css",
})
export class MemeDetailsComponent {
  @Input({ required: false }) meme: Meme | undefined;
}
