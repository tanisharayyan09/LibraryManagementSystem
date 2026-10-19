import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.util.Optional;

public class Main extends Application {

    private static final String URL =
        "jdbc:mysql://localhost:3306/librarymanagement";
    private static final String USER = "root";
    private static final String PASS =
        System.getenv("DB_PASSWORD") != null
            ? System.getenv("DB_PASSWORD")
            : "tanisha@09";

    private Stage stage;
    private BorderPane root;

    private Label totalBooks = new Label("0");
    private Label availableBooks = new Label("0");
    private Label issuedBooks = new Label("0");
    private Label statusLabel =
        new Label("Welcome to Library Management");

    // DATABASE CONNECTION
    private Connection connect() throws SQLException {
        if (PASS == null || PASS.isEmpty()) {
            throw new SQLException(
                "DB_PASSWORD is not set in the terminal."
            );
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        root = new BorderPane();

        createTableIfMissing();
        root.setLeft(createSidebar());
        showDashboard();

        Scene scene = new Scene(root, 1000, 650);
        stage.setTitle("Library Management System");
        stage.setScene(scene);
        stage.show();
    }

    // CREATE TABLE
    private void createTableIfMissing() {
        String sql =
            "CREATE TABLE IF NOT EXISTS lib_books (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "title VARCHAR(200) NOT NULL, " +
            "author VARCHAR(150) NOT NULL, " +
            "issued BOOLEAN DEFAULT FALSE)";

        try (Connection c = connect();
             Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException ex) {
            show(Alert.AlertType.ERROR,
                "Database connection error: " + ex.getMessage());
        }
    }

    // SIDEBAR
    private VBox createSidebar() {
        VBox side = new VBox(15);
        side.setPadding(new Insets(25));
        side.setPrefWidth(220);
        side.setStyle("-fx-background-color: #14243b;");

        Label logo = new Label("📚 My Library");
        logo.setStyle(
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;"
        );

        Button dashboard = menuButton("Dashboard");
        Button add = menuButton("Add Book");
        Button view = menuButton("View Books");
        Button issue = menuButton("Issue Book");
        Button ret = menuButton("Return Book");

        dashboard.setOnAction(e -> showDashboard());
        add.setOnAction(e -> addBook());
        view.setOnAction(e -> viewBooks());
        issue.setOnAction(e -> changeStatus(true));
        ret.setOnAction(e -> changeStatus(false));

        side.getChildren().addAll(
            logo, new Separator(),
            dashboard, add, view, issue, ret
        );

        return side;
    }

    private Button menuButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setPadding(new Insets(12));
        b.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-cursor: hand;"
        );
        return b;
    }

    // DASHBOARD
    private void showDashboard() {
        VBox page = new VBox(25);
        page.setPadding(new Insets(30));
        page.setStyle("-fx-background-color: #f1f5f9;");

        Label heading = new Label("Library Dashboard");
        heading.setStyle(
            "-fx-font-size: 28px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #14243b;"
        );

        Label subtitle = new Label(
            "Manage your books and library activities"
        );
        subtitle.setStyle("-fx-font-size: 14px;");

        HBox cards = new HBox(18);
        cards.getChildren().addAll(
            statCard("Total Books", totalBooks, "#2563eb"),
            statCard("Available Books", availableBooks, "#16a34a"),
            statCard("Issued Books", issuedBooks, "#ea580c")
        );

        for (javafx.scene.Node node : cards.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        Label quick = new Label("Quick Actions");
        quick.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;"
        );

        Button add = new Button("＋  Add New Book");
        Button view = new Button("▤  View All Books");
        Button issue = new Button("↗  Issue Book");
        Button ret = new Button("↩  Return Book");

        styleAction(add);
        styleAction(view);
        styleAction(issue);
        styleAction(ret);

        add.setOnAction(e -> addBook());
        view.setOnAction(e -> viewBooks());
        issue.setOnAction(e -> changeStatus(true));
        ret.setOnAction(e -> changeStatus(false));

        HBox actions = new HBox(12, add, view, issue, ret);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(8);
        info.setPadding(new Insets(20));
        info.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;"
        );

        Label infoTitle = new Label("Library Information");
        infoTitle.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold;"
        );

        info.getChildren().addAll(
            infoTitle,
            new Label("Use the menu to manage your library."),
            statusLabel
        );

        page.getChildren().addAll(
            heading, subtitle, cards, quick, actions, info
        );

        root.setCenter(page);
        refreshStats();
    }

    private VBox statCard(String name, Label number, String color) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(22));
        card.setMinWidth(150);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 12;"
        );

        Label label = new Label(name);
        label.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #64748b;"
        );

        number.setStyle(
            "-fx-font-size: 30px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + color + ";"
        );

        card.getChildren().addAll(label, number);
        return card;
    }

    private void styleAction(Button b) {
        b.setPadding(new Insets(12));
        b.setStyle(
            "-fx-background-color: #2563eb;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 8;" +
            "-fx-cursor: hand;"
        );
    }

    // REFRESH DASHBOARD
    private void refreshStats() {
        String sql =
            "SELECT COUNT(*) AS total, " +
            "SUM(CASE WHEN issued = FALSE THEN 1 ELSE 0 END) AS available, " +
            "SUM(CASE WHEN issued = TRUE THEN 1 ELSE 0 END) AS issued " +
            "FROM lib_books";

        try (Connection c = connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                totalBooks.setText(
                    String.valueOf(rs.getInt("total"))
                );
                availableBooks.setText(
                    String.valueOf(rs.getInt("available"))
                );
                issuedBooks.setText(
                    String.valueOf(rs.getInt("issued"))
                );
            }
        } catch (SQLException ex) {
            statusLabel.setText(
                "Database error: " + ex.getMessage()
            );
        }
    }

    // ASK INPUT
    private String ask(String header) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Library Management");
        d.setHeaderText(header);

        Optional<String> r = d.showAndWait();

        return r.map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

    // ADD BOOK
    private void addBook() {
        String title = ask("Enter book title");
        if (title == null) return;

        String author = ask("Enter author name");
        if (author == null) return;

        String sql =
            "INSERT INTO lib_books(title, author) VALUES(?, ?)";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, author);
            ps.executeUpdate();

            show(Alert.AlertType.INFORMATION,
                "Book added successfully!");

            statusLabel.setText("New book added: " + title);
            refreshStats();

        } catch (SQLException ex) {
            show(Alert.AlertType.ERROR,
                "Database error: " + ex.getMessage());
        }
    }

    // VIEW BOOKS
    private void viewBooks() {
        TableView<Book> table = new TableView<>();

        TableColumn<Book, Number> idCol =
            new TableColumn<>("ID");
        idCol.setCellValueFactory(
            data -> new javafx.beans.property.SimpleIntegerProperty(
                data.getValue().id
            )
        );

        TableColumn<Book, String> titleCol =
            new TableColumn<>("Title");
        titleCol.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().title
            )
        );

        TableColumn<Book, String> authorCol =
            new TableColumn<>("Author");
        authorCol.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().author
            )
        );

        TableColumn<Book, String> statusCol =
            new TableColumn<>("Status");
        statusCol.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().issued ? "Issued" : "Available"
            )
        );

        table.getColumns().addAll(
            idCol, titleCol, authorCol, statusCol
        );

        idCol.setPrefWidth(65);
        titleCol.setPrefWidth(180);
        authorCol.setPrefWidth(160);
        statusCol.setPrefWidth(110);

        try (Connection c = connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT * FROM lib_books ORDER BY id"
             )) {

            while (rs.next()) {
                table.getItems().add(
                    new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getBoolean("issued")
                    )
                );
            }

        } catch (SQLException ex) {
            show(Alert.AlertType.ERROR,
                "Database error: " + ex.getMessage());
            return;
        }

        Stage window = new Stage();
        window.setTitle("All Library Books");

        Label title = new Label("All Books");
        title.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #14243b;"
        );

        VBox layout = new VBox(18, title, table);
        layout.setPadding(new Insets(25));
        layout.setStyle("-fx-background-color: #f1f5f9;");

        VBox.setVgrow(table, Priority.ALWAYS);

        window.setScene(new Scene(layout, 650, 450));
        window.show();
    }

    // ISSUE OR RETURN BOOK
    private void changeStatus(boolean issue) {
        String idText = ask(
            issue ? "Enter Book ID to issue"
                  : "Enter Book ID to return"
        );

        if (idText == null) return;

        int id;
        try {
            id = Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            show(Alert.AlertType.WARNING,
                "Please enter a valid book ID.");
            return;
        }

        String sql =
            "UPDATE lib_books SET issued = ? " +
            "WHERE id = ? AND issued = ?";

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setBoolean(1, issue);
            ps.setInt(2, id);
            ps.setBoolean(3, !issue);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                show(Alert.AlertType.INFORMATION,
                    issue ? "Book issued successfully!"
                          : "Book returned successfully!");

                statusLabel.setText(
                    issue ? "Book issued. ID: " + id
                          : "Book returned. ID: " + id
                );

                refreshStats();
            } else {
                show(Alert.AlertType.WARNING,
                    issue
                    ? "Book not found or already issued."
                    : "Book not found or not issued.");
            }

        } catch (SQLException ex) {
            show(Alert.AlertType.ERROR,
                "Database error: " + ex.getMessage());
        }
    }

    // ALERT
    private void show(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.setTitle("Library Management");
        a.setHeaderText(null);
        a.showAndWait();
    }

    // BOOK CLASS
    public static class Book {
        int id;
        String title;
        String author;
        boolean issued;

        Book(int id, String title, String author,
             boolean issued) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.issued = issued;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}