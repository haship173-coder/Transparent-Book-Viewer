# Transparent Book & Art Viewer

This repository contains the source code for **Transparent**, a desktop application that allows users to view and manage digital books and artwork on Windows.  The goal of this project is to provide a clean and simple interface for opening PDFs, images and other common formats while tracking your reading progress and favourites.

## Main features

- **Username login** – the application only asks for a username to identify you; there is no password system.
- **Open files** – view PDF, image (JPEG/PNG) and other formats.  When you close a file the current page number is saved automatically.
- **Search** – search your library by title.
- **Reading history** – automatically save the last page viewed for each book/comic so you can resume later.
- **Favourites** – mark items as favourites for quick access.

## Technology

This project is built using the following technologies:

- **Java 17**
- **JavaFX 17** for the user interface
- **SQL Server Express** for data storage (users, contents, history and favourites)
- **Apache PDFBox** for rendering PDF files
- **JavaFX ImageView** for displaying images

## Project structure

The Maven project is organised into packages following a typical layered architecture.  Models represent your data, DAOs handle database access, services encapsulate business logic and controllers respond to UI events.

```
Transparent/
├── README.md
├── pom.xml
└── src
    └── main
        ├── java
        │   └── transparent
        │       ├── MainApp.java
        │       ├── db
        │       │   └── DBConnectionManager.java
        │       ├── model
        │       │   ├── User.java
        │       │   ├── Content.java
        │       │   ├── HistoryRecord.java
        │       │   └── Favourite.java
        │       ├── dao
        │       │   ├── UserDAO.java
        │       │   ├── ContentDAO.java
        │       │   ├── HistoryDAO.java
        │       │   └── FavouriteDAO.java
        │       ├── service
        │       │   ├── UserService.java
        │       │   ├── ContentService.java
        │       │   ├── HistoryService.java
        │       │   └── FavouriteService.java
        │       └── controller
        │           ├── CurrentUser.java
        │           ├── LoginController.java
        │           ├── MainController.java
        │           ├── ReaderController.java
        │           ├── HistoryController.java
        │           └── FavouritesController.java
        └── resources
            ├── login.fxml
            ├── main.fxml
            ├── reader.fxml
            ├── history.fxml
            └── favourites.fxml
```

Feel free to extend the skeleton classes with your own logic.  See the `docs` folder or your assignment brief for further design notes.