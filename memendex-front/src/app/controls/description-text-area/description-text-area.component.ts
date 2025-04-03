import {
  AfterViewInit,
  Component,
  ElementRef,
  input,
  OnInit,
  Renderer2,
  signal,
  ViewChild,
} from "@angular/core";
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from "@angular/forms";

@Component({
  selector: "app-description-text-area",
  imports: [],
  templateUrl: "./description-text-area.component.html",
  styleUrl: "./description-text-area.component.css",
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: DescriptionTextAreaComponent,
    },
  ],
})
export class DescriptionTextAreaComponent
  implements AfterViewInit, ControlValueAccessor
{
  @ViewChild("textArea", { static: true, read: ElementRef })
  textArea?: ElementRef;
  minLines = input(3);
  readOnly = input(false);
  numberOfLines = signal(0);

  // ControlValueAcessor callbacks
  onChange = (description: string) => {};
  onTouched = () => {};

  constructor(private _renderer: Renderer2) {}

  writeValue(description: string): void {
    this._renderer.setProperty(this.nativeTextArea, "value", description);
    if (this.nativeTextArea)
      this.nativeTextArea.parentElement!.dataset["replicatedValue"] =
        this.nativeTextArea.value;
  }

  registerOnChange(onChange: any): void {
    this.onChange = onChange;
  }

  registerOnTouched(onTouched: any): void {
    this.onTouched = onTouched;
  }

  setDisabledState?(isDisabled: boolean): void {
    this._renderer.setProperty(this.nativeTextArea, "readOnly", isDisabled);
    // this.nativeTextArea.readOnly = isDisabled;
  }

  ngAfterViewInit(): void {
    this.nativeTextArea.parentElement!.dataset["replicatedValue"] =
      this.nativeTextArea.value;
  }

  get nativeTextArea(): HTMLTextAreaElement {
    return this.textArea?.nativeElement as HTMLTextAreaElement;
  }

  onKeyUp(event: KeyboardEvent) {
    const native = this.nativeTextArea;

    const countOccurence = (str: string, char: string): number => {
      let count = 0;
      for (let i = 0; i < str.length; i++) {
        if (str[i] === char) {
          count++;
        }
      }
      return count;
    };

    const count = Math.max(this.minLines(), this.countLines(native));

    this.numberOfLines.set(count);

    native.rows = count;
  }

  private countLines(textArea: HTMLTextAreaElement): number {
    const realLines = textArea.value.split("\n");
    const realLinesLength = realLines.map((line) => line.length);
    return realLinesLength
      .map((length) => {
        console.log(`length: ${length} / cols: ${textArea.cols}`);
        return length > textArea.cols
          ? Math.floor(length / textArea.cols) + 1
          : 1;
      })
      .reduce((l1, l2) => l1 + l2);
  }
}
