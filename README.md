# Memendex

Memendex is a remote file system with the primary purpose of storing memes. It has the ability to add metadata, like tags and description, so you never have to scroll your memes library to find again that meme for that very specific ocasion.

## Building

You can build this project using npm commands that will redirect command to the subprojects inside this monorepo.

- `npm run build:front`: Builds the `dist` directory of the Angular app
- `npm run copy:static`: Copies the `dist` directory to Spring Boot server's resource directory
- `npm run build:server`: Builds the `.jar` file of the Spring Boot server

## Running the application

Simply run it as java app:

```shell
java -jar memendex-server/target/memendex-server-*.jar
```

You can configure the instance using environment variables:

| Variable                   | Description                                           | Default     |
| ---                        | ---                                                   | ---         |
| `MEMENDEX_PORT`            | The application http port                             | `8080`      |
| `MEMENDEX_UPLOAD_LOCATION` | Directory which memendex will store the images        | `./uploads` |
| `MEMENDEX_CACHE`           | Cache directory. This directory may be safely deleted | `.cache`    |

## Screenshots

![index page screenshot](screenshots/index-page.png)

## Roadmap

- [ ] Support for markdown in description field (with markdown preview)
- [ ] Tags and scoped tags (e.g. #tag, #scoped:tag)
- [ ] Meme templates support
  - [ ] Create new memes on the fly from a template
  - [ ] View memes created from a template
- [ ] Bookmark like features
- [ ] OCR support
