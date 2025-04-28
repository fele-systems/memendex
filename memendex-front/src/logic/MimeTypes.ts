// TODO: Fill this from server
export var SUPPORTED_THUMBNAILS: string[] = [];

async function getKnownExtensions() {
  const response = await fetch("/api/mime/known");

  if (response.ok) {
    SUPPORTED_THUMBNAILS = (await response.json()) as string[];
  } else {
    console.error(
      "Could not fetch list of supported thumbnail extensions. Limiting to known image types.",
    );
    SUPPORTED_THUMBNAILS = ["jpeg", "png", "gif"];
  }
}

getKnownExtensions();
