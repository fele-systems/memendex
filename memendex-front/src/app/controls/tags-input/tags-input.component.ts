import { ɵgetParentElement } from "@angular/animations/browser";
import { HttpClient } from "@angular/common/http";
import { Component, computed, input, OnInit, signal } from "@angular/core";
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from "@angular/forms";

type TagUsage = { tag: string; count: number };

@Component({
  selector: "app-tags-input",
  imports: [],
  templateUrl: "./tags-input.component.html",
  styleUrl: "./tags-input.component.css",
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: TagsInputComponent,
    },
  ],
})
export class TagsInputComponent implements ControlValueAccessor, OnInit {
  tags = signal<string[]>([]);
  disabled: boolean = false;
  onChange = (tags: string[]) => {};
  onTouched = (tags: string[]) => {};

  suggestedTags = signal([] as TagUsage[]);

  autoCompletion = computed(() =>
    this.suggestedTags().filter(
      (tag) => !this.tags().some((x) => tag.tag === x),
    ),
  );

  constructor(private http: HttpClient) {}
  ngOnInit(): void {
    this.searchTags("");
  }

  private searchTags(searchTerm: string) {
    this.http
      .get("/api/tags/suggestions", {
        params: { q: searchTerm },
        observe: "response",
      })
      .subscribe((response) => {
        this.suggestedTags.set(response.body as TagUsage[]);
      });
  }

  writeValue(tags: string[]): void {
    this.tags.set(tags);
    this.searchTags("");
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  addTag(event: Event) {
    event.preventDefault();
    var input = event.target as HTMLInputElement;
    var value = input.value;
    input.value = "";
    this.searchTags("");

    if (!value.startsWith("#")) value = `#${value}`;
    this.tags.update((tags) => [...tags, value]);
    this.onChange(this.tags());
  }

  deleteTag(event: Event) {
    var target = event.target as HTMLElement;
    // target.parentNode?.removeChild(target);
    var i = this.tags().findIndex((tag) => tag === target.innerText);
    if (i >= 0) {
      var tmp = Object.assign([], this.tags());
      tmp.splice(i, 1);
      this.tags.set(tmp);
    }
    this.onChange(this.tags());
  }

  onFocus(event: Event) {
    var target = event.target as HTMLElement;

    target.parentElement?.parentElement?.classList.add("focus");
  }

  onBlur(event: FocusEvent) {
    var target = event.target as HTMLElement;

    //target.parentElement?.parentElement?.classList.remove("focus");
    target.parentElement?.classList.remove("focus");
  }

  onInput(event: Event) {
    console.log("Typed something");
    this.searchTags((event.currentTarget as HTMLInputElement).value);
  }

  acceptSuggestion(event: Event) {
    var target = event.currentTarget as HTMLElement;

    var suggestion = target.getElementsByClassName("stat-tag")[0].innerHTML;
    this.tags.update((tags) => [...tags, suggestion]);
    this.onChange(this.tags());
    this.searchTags("");
  }
}
